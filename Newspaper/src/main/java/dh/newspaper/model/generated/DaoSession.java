package dh.newspaper.model.generated;

import android.database.sqlite.SQLiteDatabase;

import java.util.Map;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.AbstractDaoSession;
import de.greenrobot.dao.identityscope.IdentityScopeType;
import de.greenrobot.dao.internal.DaoConfig;

import dh.newspaper.model.generated.Article;
import dh.newspaper.model.generated.Subscription;

import dh.newspaper.model.generated.ArticleDao;
import dh.newspaper.model.generated.SubscriptionDao;

// THIS CODE IS GENERATED BY greenDAO, DO NOT EDIT.

/**
 * {@inheritDoc}
 * 
 * @see de.greenrobot.dao.AbstractDaoSession
 */
public class DaoSession extends AbstractDaoSession {

    private final DaoConfig articleDaoConfig;
    private final DaoConfig subscriptionDaoConfig;

    private final ArticleDao articleDao;
    private final SubscriptionDao subscriptionDao;

    public DaoSession(SQLiteDatabase db, IdentityScopeType type, Map<Class<? extends AbstractDao<?, ?>>, DaoConfig>
            daoConfigMap) {
        super(db);

        articleDaoConfig = daoConfigMap.get(ArticleDao.class).clone();
        articleDaoConfig.initIdentityScope(type);

        subscriptionDaoConfig = daoConfigMap.get(SubscriptionDao.class).clone();
        subscriptionDaoConfig.initIdentityScope(type);

        articleDao = new ArticleDao(articleDaoConfig, this);
        subscriptionDao = new SubscriptionDao(subscriptionDaoConfig, this);

        registerDao(Article.class, articleDao);
        registerDao(Subscription.class, subscriptionDao);
    }
    
    public void clear() {
        articleDaoConfig.getIdentityScope().clear();
        subscriptionDaoConfig.getIdentityScope().clear();
    }

    public ArticleDao getArticleDao() {
        return articleDao;
    }

    public SubscriptionDao getSubscriptionDao() {
        return subscriptionDao;
    }

}
