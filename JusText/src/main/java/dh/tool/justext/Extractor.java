package dh.tool.justext;

import com.google.common.base.Stopwatch;
import com.google.common.base.Strings;
import dh.tool.common.PerfWatcher;
import dh.tool.common.StrUtils;
import dh.tool.jsoup.NodeHelper;
import dh.tool.thread.ICancellation;
import dh.tool.thread.ThreadUtils;
import org.jsoup.nodes.*;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.LinkedList;

public class Extractor {
	private static final Logger log = LoggerFactory.getLogger(Extractor.class);

	private PerfWatcher pw;
	private Configuration conf;
	private final ICancellation cancellation;

	public Extractor() {
		this(Configuration.DEFAULT, null);
	}
	public Extractor(Configuration conf) {
		this(conf, null);
	}
	public Extractor(Configuration conf, ICancellation cancellation) {
		this.conf = conf;
		this.cancellation = cancellation;
	}

	/**
	 * The document will be at dirty state if cancel happen
	 * throws CancellationException
	 */
	public void removeBoilerplate(Document document) {
		process(document, false);
	}
	/**
	 * The document will be at dirty state if cancel happen
	 * throws CancellationException
	 */
	public void decorateBoilerplate(Document document) {
		process(document, true);
	}

	/**
	 * throws CancellationException
	 */
	private void process(Document document, boolean colorize) {
		ThreadUtils.checkCancellation(cancellation);
		
		Stopwatch sw = Stopwatch.createStarted();
		pw = new PerfWatcher(log, document.baseUri());

		document.outputSettings().escapeMode(Entities.EscapeMode.xhtml);

		String lang = null;
		if (conf.autoDetectLanguage() && Strings.isNullOrEmpty(conf.language())) {
			//auto detect language only if user did not configured language
			lang = NodeHelper.detectLanguage(document);
			pw.t("Detect Language");

			if (!Strings.isNullOrEmpty(lang)) {
				//found language, check if we have stop words list on this language
				lang = StopwordsManager.getLanguage(lang.toLowerCase());
				if (!Strings.isNullOrEmpty(lang)) {
					/*
					client did not configured the language but we detected the language of the page and we have stop words list on it
					so we will exceptionally change the language configuration by cloning the actual config that the client gave us
					*/
					conf = (new Configuration.Builder(conf)).language(lang).build();
					pw.debug("Found language");
				}
			}
		}

		if (conf.preCleanUselessContent()) {
			ThreadUtils.checkCancellation(cancellation);
			cleanUselessContent(document);
			pw.t("cleanUselessContent");
		}


		ThreadUtils.checkCancellation(cancellation);
		LinkedList<Paragraph> paragraphs;
		if (conf.processOnlyBody()) {
			Node n = document.body();
			pw.t("Get document body");
			paragraphs = computeParagraphs(n);
		}
		else {
			paragraphs = computeParagraphs(document);
		}
		pw.t("Found "+paragraphs.size()+" paragraphs");

		new QualityComputation(paragraphs).process();

		ThreadUtils.checkCancellation(cancellation);

		if (colorize) {
			for (Paragraph p : paragraphs) {
				p.colorizeParagraph();
			}
			pw.t("Colorize paragraph");
		}
		else {
			//remove all BAD node paragraphs
			for (Paragraph p : paragraphs) {
				if (p.getQuality()== Paragraph.Quality.BAD) {
					for (Node n : p) {
						n.remove();
					}
				}
			}
			pw.t("Remove BAD paragraphs");

			ThreadUtils.checkCancellation(cancellation);

			//clean empty tags
			if (conf.postCleanBoilerplateTags()) {
				NodeHelper.cleanEmptyElements(document);
				pw.t("Clean empty elements");
				NodeHelper.unwrapRedundancyTags(document);
				pw.t("Unwrap redundancies tags");
			}
		}

		pw.dg("Finished");
	}

	/**
	 * Clean useless tags, attributes and replace absolute path
	 */
	public void cleanUselessContent(Node n) {
		new NodeTraversor(new NodeVisitor() {
			@Override
			public void head(Node node, int depth) {
				for (int i = 0; i < node.childNodes().size();) {
					Node child = node.childNode(i);

					//remove useless Tags
					if (NodeHelper.isIgnorableTagNode(child)) {
						child.remove();
					}
					else {
						i++;
					}
				}

				boolean isImgTag = NodeHelper.isImgTag(node);

				/*
				* For img tag, we keep all attributes, for other tag, we remove every useless Attributes.
				* Because in some website, eg: http://www.huffingtonpost.com/2014/07/11/clinton-2016-media-coverage_n_5577900.html
				* the img source is not "src", TODO: we need a image source guesser in this case
				*/
				if (isImgTag) {
					//For img tag, we keep all attributes except style attributes if the config ask to remove all style attributes
					if (!conf.keepStyleAttribute()) {
						node.removeAttr("style");
					}
				}
				else {
					if (node instanceof Element) {
						Iterator<Attribute> it = node.attributes().iterator();
						while (it.hasNext()) {
							Attribute attr = it.next();
							if (NodeHelper.isIgnorableAttribute(attr.getKey())) {
								node.removeAttr(attr.getKey());
							}

							if ("style".equalsIgnoreCase(attr.getKey())) {
								if (conf.keepStyleAttribute()) {
									//keep style attribute but force display hidden nodes: remove the attributes style which contains "display:none".
									// So the hidden content will visible with no style
									//eg: http://edition.cnn.com/2014/07/11/sport/football/world-cup-pleitgen-german-words/index.html?hpt=hp_c2
									String styleAttrValue = attr.getValue();
									if (styleAttrValue.replace(" ", "").toLowerCase().contains("display:none")) {
										node.removeAttr(attr.getKey());
									}
								}
								else {
									node.removeAttr(attr.getKey()); //remove the style attribute
								}
							}
						}
					}
				}


				/*
				 * Convert relative path to absolute path
				 */
				if (isImgTag) {
					//TODO: img src guesser, to guess what is the real source of the image tag remove twitter, facebook,.. logo, blank image or too small image
					String absolutePath = node.attr("abs:src");
					if (!Strings.isNullOrEmpty(absolutePath)) {
						node.attr("src", absolutePath);
					}
				}
				else if (NodeHelper.isLinkTag(node)) {
					String absolutePath = node.attr("abs:href");
					if (!Strings.isNullOrEmpty(absolutePath)) {
						node.attr("href", absolutePath);
					}
				}
			}

			@Override
			public void tail(Node node, int depth) {

			}
		}).traverse(n);
	}

	private LinkedList<Paragraph> computeParagraphs(Node node) {
		ParagraphsExplorer pe = new ParagraphsExplorer(conf);
		node.traverse(pe);
		return pe.getParagraphs();
	}

	private static boolean nearlyIdenticalParagraph(Paragraph p1, Paragraph p2) {
		if (p1.isImage() && p2.isImage()) {
			return StrUtils.equalsIgnoreCases(p1.getImageUrl(), p2.getImageUrl());
		}
		if (p1.getLength()>0 && StrUtils.equalsString(p1.getRawText(), p2.getRawText())) {
			return p1.isHeading() == p2.isHeading();
		}
		return false;
	}

	private class QualityComputation {
		private final LinkedList<Paragraph> paragraphs;
		private Paragraph title;

		public QualityComputation(LinkedList<Paragraph> paragraphs) {
			this.paragraphs = paragraphs;
		}

		public void process() {
			ThreadUtils.checkCancellation(cancellation);

			//performs context-free classification
			for (Paragraph p : paragraphs) {
				ThreadUtils.checkCancellation(cancellation);
				p.initRawInfo();
			}
			pw.t("Compute free-context quality");

			//pre-process heading

			ThreadUtils.checkCancellation(cancellation);

			if (conf.contentAlwaysHasTitle()) {
				preProcessHeading2();
			}
			else {
				preProcessHeading();
			}

			//context-sensitive classification

			processEdges(paragraphs.iterator()); //top edge
			pw.t("Process top edge");

			processEdges(paragraphs.descendingIterator()); //bottom edge
			pw.t("Process bottom edge");

			removeRedundancyParagraphs();

			int left;
			for (int i=0; i<paragraphs.size();) {
				ThreadUtils.checkCancellation(cancellation);
				if (paragraphs.get(i).isNearOrShort()) {
					left = i-1;
					while (paragraphs.get(i).isNearOrShort() && i<paragraphs.size()) {
						i++;
					}
					processChunk(left, i);
				}
				i++;
			}
			pw.t("Compute context-sensitive quality");

			//post-process heading

			postProcessHeading();

			if (conf.contentAlwaysHasTitle()) {
				title = findTitle();
				pw.t("Find title");
			}

			removeTrailHeading();

			fillHoles();

			//TODO: recognize image+SHORT paragraphs because they are often images + caption
			//TODO: tolerate some short paragraphs because they are often name of authors


			if (conf.removeTitle()) {
				//find first good heading
				for (int i=0; i<paragraphs.size(); i++) {
					ThreadUtils.checkCancellation(cancellation);

					Paragraph p = paragraphs.get(i);
					if (p.isH1orH2() && p.getQuality()== Paragraph.Quality.GOOD) {
						while (p.isH1orH2() && p.getQuality()== Paragraph.Quality.GOOD) {
							title = p;
							p.setQuality(Paragraph.Quality.BAD, "RemoveTitle");
							i++;
							p = paragraphs.get(i);
						}
						break;
					}
				}
				pw.t("Remove title");
			}
		}

		/**
		 * Remove all identical images and paragraphs. They are always boiler plates
		 */
		public void removeRedundancyParagraphs() {
			ThreadUtils.checkCancellation(cancellation);
			pw.resetStopwatch();
			for (int i=0; i<paragraphs.size()-1; i++) {
				Paragraph p1 = paragraphs.get(i);
				for (int j = i + 1; j < paragraphs.size(); j++) {
					Paragraph p2 = paragraphs.get(j);
					if (nearlyIdenticalParagraph(p1, p2)) {
						p1.setContextFreeQuality(Paragraph.Quality.BAD, "identical paragraph "+p2.getId());
						p1.setQuality(Paragraph.Quality.BAD, "identical paragraph "+p2.getId());
						p2.setContextFreeQuality(Paragraph.Quality.BAD, "identical paragraph "+p1.getId());
						p2.setQuality(Paragraph.Quality.BAD, "identical paragraph "+p1.getId());
					}
				}
			}
			pw.t("removeRedundancyParagraphs");
		}

		/**
		 * jusText algorithm find all SHORT heading paragraph which is not too far away from the first GOOD paragraph
		 * promote these heading from SHORT to NEAR_GOOD
		 */
		public void preProcessHeading() {
			if (!conf.processHeadings()) {
				return;
			}
			//find all SHORT heading paragraph which is not too far away from the first GOOD paragraph
			for (int i=0; i<paragraphs.size(); i++) {
				Paragraph shortHeading = paragraphs.get(i);
				if (shortHeading.isHeading() && shortHeading.getContextFreeQuality()==Paragraph.Quality.SHORT) {
					int distanceToFirstGood = 0;
					for (int j=i+1; j<paragraphs.size() && distanceToFirstGood<conf.maxHeadingDistance(); j++) {
						Paragraph p = paragraphs.get(j);
						if (p.getContextFreeQuality() == Paragraph.Quality.GOOD) {
							shortHeading.setContextFreeQuality(Paragraph.Quality.NEAR_GOOD, "Pre-heading");
							break;
						}
						distanceToFirstGood += p.getLength();
					}
				}
			}
			pw.t("Promote SHORT heading near a GOOD paragraph");
		}

		/**
		 * enhance by Hiep after trying: http://www.huffingtonpost.com/2014/06/25/googles-massive-plan-to-t_n_5530653.html
		 * give more tolerance for NEAR-GOOD text after a heading paragraph.
		 *
		 * After heading, we often have a small resume paragraph (excerpt) which is sometimes NEAR_GOOD
		 * In this case, we will force it to GOOD (context-free) and promote SHORT heading to NEAR_GOOD
		 */
		public void preProcessHeading2() {
			if (!conf.processHeadings()) {
				return;
			}

			//find all SHORT heading paragraph which is not too far away from the first GOOD or NEAR_GOOD paragraph
			for (int i=0; i<paragraphs.size(); i++) {
				Paragraph shortHeading = paragraphs.get(i);
				if (shortHeading.isHeading() && shortHeading.getContextFreeQuality()==Paragraph.Quality.SHORT) {
					int distanceToFirstGood = 0;
					for (int j=i+1; j<paragraphs.size() && distanceToFirstGood<conf.maxHeadingDistance(); j++) {
						Paragraph p = paragraphs.get(j);
						if (p.getContextFreeQuality() == Paragraph.Quality.GOOD) {
							//a SHORT heading near a GOOD paragraph: normal jusText processing
							shortHeading.setContextFreeQuality(Paragraph.Quality.NEAR_GOOD, "Pre-heading");
							break;
						}
						if (p.getContextFreeQuality() == Paragraph.Quality.NEAR_GOOD) {
							if (shortHeading.isH1orH2()) {
								//a SHORT heading near a NEAR_GOOD paragraph: excerpt detected
								shortHeading.setContextFreeQuality(Paragraph.Quality.NEAR_GOOD, "Pre-heading-tolerance-h1h2");
								p.setContextFreeQuality(Paragraph.Quality.GOOD, "Pre-heading-excerpt");
							}
						}
						distanceToFirstGood += p.getLength();
					}
				}
			}
			pw.t("Promote SHORT heading near a GOOD/NEAR_GOOD paragraph");

			//last chance for title: promote nearest NOT_BAD h1 to GOOD
			Paragraph nearestH1 = null;
			for (int i=0; i<paragraphs.size(); i++) {
				Paragraph pf = paragraphs.get(i);

				if (pf.isH1() && pf.getContextFreeQuality()!= Paragraph.Quality.BAD) {
					if (pf.getQuality()== Paragraph.Quality.GOOD) {
						break; //nothing to do, we had the title
					}
					else {
						nearestH1 = pf;
					}
				}
				if (pf.getQuality() == Paragraph.Quality.GOOD && nearestH1!=null) {
					nearestH1.setContextFreeQuality(Paragraph.Quality.GOOD, "Pre-heading-h1");
					break;
				}
			}
			pw.t("Tolerate *h1* title");
		}

		/**
		 * make sure that we has a title in article
		 * if we did not have any heading GOOD paragraph, so
		 * we will find the nearest context-free NEAR_GOOD heading before the first GOOD paragraph.
		 * We will use this paragraph as Title, so promote it to GOOD.
		 */
		public Paragraph findTitle() {
			ThreadUtils.checkCancellation(cancellation);

			for (Paragraph p : paragraphs) {
				if (p.isH1orH2() && p.getQuality() == Paragraph.Quality.GOOD) {
					//we already has a GOOD heading paragraph, nothing to do here
					return p;
				}
			}

			//find the nearest NEAR_GOOD heading before the first GOOD paragraph
			Paragraph lastNearGoodHeading = null;
			for (int i=0; i<paragraphs.size(); i++) {
				Paragraph p = paragraphs.get(i);
				if (p.isH1orH2() && p.getContextFreeQuality() == Paragraph.Quality.NEAR_GOOD) {
					lastNearGoodHeading = p;
				}
				if (p.getQuality() == Paragraph.Quality.GOOD) {
					break;
				}
			}

			//promote it to GOOD (so we will have title)
			if (lastNearGoodHeading != null) {
				lastNearGoodHeading.setQuality(Paragraph.Quality.GOOD, "FindTitle");
				return lastNearGoodHeading;
			}
			return null;
		}

		public void postProcessHeading() {
			ThreadUtils.checkCancellation(cancellation);

			if (!conf.processHeadings()) {
				return;
			}

			pw.resetStopwatch();
			//find all heading paragraph NON-BAD in Context-free which is not too far away from the first GOOD paragraph
			for (int i=0; i<paragraphs.size(); i++) {
				Paragraph nonBadHeading = paragraphs.get(i);
				if (nonBadHeading.isH1orH2()) {
					if (nonBadHeading.getQuality() == Paragraph.Quality.GOOD) {
						//the title is already here, no need to search further
						return;
					}
					if (nonBadHeading.getContextFreeQuality()!=Paragraph.Quality.BAD) {
						int distanceToFirstGood = 0;
						for (int j = i + 1; j < paragraphs.size() && distanceToFirstGood < conf.maxHeadingDistance(); j++) {
							Paragraph p = paragraphs.get(j);
							if (p.getContextFreeQuality() == Paragraph.Quality.GOOD) {
								nonBadHeading.setQuality(Paragraph.Quality.GOOD, "Post-heading");
								break;
							}
							distanceToFirstGood += p.getLength();
						}
					}
				}
			}
			pw.t("Post process heading");
		}

		/**
		 * conf.processHeadings() && conf.contentAlwaysHasTitle() force the extractor find and promote heading h1, h2
		 * sometimes it tolerate too much the heading paragraph and promote boiler plates heading at the end of the article
		 * this function will remove all heading at the end of the article
		 * Good-Good-headingToRemove-Bad-Bad-HeadingToRemove-Bad-Bad-Bad
		 */
		public void removeTrailHeading() {
			if (conf.processHeadings() && conf.contentAlwaysHasTitle()) {
				ThreadUtils.checkCancellation(cancellation);
				pw.resetStopwatch();
				Iterator<Paragraph> it = paragraphs.descendingIterator();
				//Start from bottom to top, remove heading which trail the articles
				//after these trail headings there are only empty or bad paragraphs
				while (it.hasNext()) {
					Paragraph p = it.next();
					if (p.isHeading() && p.getQuality()== Paragraph.Quality.GOOD) {
						p.setQuality(Paragraph.Quality.BAD, "remove trail headings");
						continue;
					}
					//jump over empty and BAD paragraphs
					if (p.getQuality()== Paragraph.Quality.BAD || (!p.isImage() && p.getLength()==0))
						continue;
					break;
				}
				pw.t("remove trail headings");
			}
		}

		/**
		 * If there is a big holes between two GOOD paragraphs. it means The distant between 2 GOOD paragraphs is
		 * very far away with many NEAR_GOOD content in the middle, in this case, we will turn all the NEAR_GOOD
		 * to GOOD to fill the holes.
		 *
		 * - holeSurface = distant between 2 GOOD paragraphs
		 * -
		 * This method find big holes (holeSurface > conf.maxHeadingDistance), compute the the density of NEAR_GOOD content
		 * in this holes, if half of the holes is NEAR_GOOD (conf.NearGoodDensityRequiredToFillHoles=0.7), so we will promote all the
		 * NEAR_GOOD to GOOD
		 */
		private void fillHoles() {
			ThreadUtils.checkCancellation(cancellation);

			if (conf.nearGoodDensityRequiredToFillHoles() > 1) {
				return;
			}

			int n = paragraphs.size();

			for (int i = 0; i<n-1; i++) {
				Paragraph p1 = paragraphs.get(i);
				if (p1.getQuality()== Paragraph.Quality.GOOD) {
					ThreadUtils.checkCancellation(cancellation);

					//find the nearest GOOD paragraph p2, and measure the distant between p1 and p2
					int holeSurface = 0; //distant between 2 GOOD paragraphs p1 and p2
					int nearGoodSurface = 0; //count NEAR_GOOD characters between p1 and p2
					Paragraph p2 = null;
					int j=i+1;
					for (;j<n; j++) {
						p2 = paragraphs.get(j);
						if (p2.getQuality()== Paragraph.Quality.GOOD) {
							break;
						}
						if (p2.getContextFreeQuality() == Paragraph.Quality.NEAR_GOOD) {
							nearGoodSurface += p2.getLength();
						}
						holeSurface += p2.getLength();
					}

					if (p2!=null && p2.getQuality()== Paragraph.Quality.GOOD) {
						if (holeSurface > conf.maxHeadingDistance()) {
							double nearGoodDensity = (double)nearGoodSurface / holeSurface;
							if (nearGoodDensity > conf.nearGoodDensityRequiredToFillHoles()) {
								//the holes is big enough to fill
								for (int k = i + 1; k < j; k++) {
									ThreadUtils.checkCancellation(cancellation);
									Paragraph p = paragraphs.get(k);
									if (p.getContextFreeQuality() == Paragraph.Quality.NEAR_GOOD) {
										p.setQuality(Paragraph.Quality.GOOD,
												String.format("Fill big holes (surface=%d > %d) between [%d] and [%d], because high nearGoodDensity=%.3g > %.3g",
														holeSurface, conf.maxHeadingDistance(), p1.getId(), p2.getId(),
														nearGoodDensity, conf.nearGoodDensityRequiredToFillHoles())
										);
									}
								}
							}
						}
						i = j-1; //jump to the next GOOD paragraph
					}
				}
			}
		}

		private void processEdges(Iterator<Paragraph> it) {
			//ThreadUtils.checkCancellation(cancellation);
			Paragraph p;
			while (it.hasNext() && (p = it.next()).isNearOrShort()) {
				ThreadUtils.checkCancellation(cancellation);
				if (conf.strictOnEdgeContent()) {
					p.setQuality(Paragraph.Quality.BAD, "ProcessEdge"); // strict way: content from edge are often boilerplate
				} else {
					//tolerate NEAR_GOOD content on edge, promote it to GOOD
					if (p.getQuality() == Paragraph.Quality.NEAR_GOOD) {
						p.setQuality(Paragraph.Quality.GOOD, "ProcessEdge"); //NEAR_GOOD becomes GOOD
					}
					else {
						p.setQuality(Paragraph.Quality.BAD, "ProcessEdge"); //SHORT becomes BAD
					}
				}
			}
		}

		private void processChunk(int leftPos, int rightPos) {
			Paragraph left = paragraphs.get(leftPos);
			Paragraph right = paragraphs.get(rightPos);

			if (sameQuality(leftPos, rightPos, left, right, Paragraph.Quality.GOOD)) {
				return;
			}
			if (sameQuality(leftPos, rightPos, left, right, Paragraph.Quality.BAD)) {
				return;
			}

			if (left.getQuality()==Paragraph.Quality.BAD) {
				/* B, S->B, N->G, ?->G, ?->G, G */

				int i;
				for (i=leftPos+1; i<rightPos; i++) {
					Paragraph p = paragraphs.get(i);
					if (p.getQuality()== Paragraph.Quality.NEAR_GOOD) //found the nearest NEAR_GOOD from the extremity BAD
						break;
					if (Configuration.DEBUG) {
						if (p.getQuality() != Paragraph.Quality.SHORT) {
							throw new IllegalStateException();
						}
					}
					p.setQuality(Paragraph.Quality.BAD, "Context-sensitive-dif");
				}

				for (int j=i; j<rightPos; j++) {
					paragraphs.get(j).setQuality(Paragraph.Quality.GOOD, "Context-sensitive-dif");
				}
				return;
			}

			if (right.getQuality()==Paragraph.Quality.BAD) {
				/* G, ?->G, ?->G, N->G, S->B, B */

				int i;
				for (i=rightPos-1; i>leftPos; i--) {
					Paragraph p = paragraphs.get(i);
					if (p.getQuality()== Paragraph.Quality.NEAR_GOOD) //found the nearest NEAR_GOOD from the extremity BAD
						break;
					if (Configuration.DEBUG) {
						if (p.getQuality() != Paragraph.Quality.SHORT) {
							throw new IllegalStateException();
						}
					}
					p.setQuality(Paragraph.Quality.BAD, "Context-sensitive-dif");
				}

				for (int j=i; j>leftPos; j--) {
					paragraphs.get(j).setQuality(Paragraph.Quality.GOOD, "Context-sensitive-dif");
				}
				return;
			}
		}

		private boolean sameQuality(int leftPos, int rightPos, Paragraph left, Paragraph right, Paragraph.Quality q) {
			if (left.getQuality()==q && right.getQuality()==q) {
				for (int i=leftPos+1; i<rightPos; i++) {
					paragraphs.get(i).setQuality(q, "Context-sensitive-eq");
				}
				return true;
			}
			return false;
		}
	}
}
