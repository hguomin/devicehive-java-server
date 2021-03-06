package com.devicehive.dao.rdbms;

import com.devicehive.dao.UserDao;
import com.devicehive.model.Network;
import com.devicehive.model.User;
import com.devicehive.vo.NetworkVO;
import com.devicehive.vo.UserVO;
import com.devicehive.vo.UserWithNetworkVO;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

@Profile({"rdbms"})
@Repository
public class UserDaoRdbmsImpl extends RdbmsGenericDao implements UserDao {

    @Override
    public Optional<UserVO> findByName(String name) {
        Optional<User> login = createNamedQuery(User.class, "User.findByName", of(CacheConfig.get()))
                .setParameter("login", name)
                .getResultList()
                .stream().findFirst();
        return optionalUserConvertToVo(login);
    }

    @Override
    public UserVO findByGoogleName(String name) {
        User user = createNamedQuery(User.class, "User.findByGoogleName", empty())
                .setParameter("login", name)
                .getResultList()
                .stream().findFirst().orElse(null);
        return User.convertToVo(user);
    }

    @Override
    public UserVO findByFacebookName(String name) {
        User user = createNamedQuery(User.class, "User.findByFacebookName", empty())
                .setParameter("login", name)
                .getResultList()
                .stream().findFirst().orElse(null);
        return User.convertToVo(user);
    }

    @Override
    public UserVO findByGithubName(String name) {
        User user = createNamedQuery(User.class, "User.findByGithubName", empty())
                .setParameter("login", name)
                .getResultList()
                .stream().findFirst().orElse(null);
        return User.convertToVo(user);
    }

    @Override
    public Optional<UserVO> findByIdentityName(String login, String googleLogin, String facebookLogin, String githubLogin) {
        Optional<User> user = createNamedQuery(User.class, "User.findByIdentityName", of(CacheConfig.bypass()))
                .setParameter("login", login)
                .setParameter("googleLogin", googleLogin)
                .setParameter("facebookLogin", facebookLogin)
                .setParameter("githubLogin", githubLogin)
                .getResultList()
                .stream().findFirst();
        return optionalUserConvertToVo(user);
    }

    @Override
    public long hasAccessToNetwork(UserVO user, NetworkVO network) {
        Network nw = reference(Network.class, network.getId());
        return createNamedQuery(Long.class, "User.hasAccessToNetwork", empty())
                .setParameter("user", user.getId())
                .setParameter("network", nw)
                .getSingleResult();
    }

    @Override
    public long hasAccessToDevice(UserVO user, String deviceGuid) {
        return createNamedQuery(Long.class, "User.hasAccessToDevice", empty())
                .setParameter("user", user.getId())
                .setParameter("guid", deviceGuid)
                .getSingleResult();
    }

    @Override
    public UserWithNetworkVO getWithNetworksById(long id) {
        User user = createNamedQuery(User.class, "User.getWithNetworksById", of(CacheConfig.refresh()))
                .setParameter("id", id)
                .getResultList()
                .stream().findFirst().orElse(null);
        if (user == null) {
            return null;
        }
        UserVO vo = User.convertToVo(user);
        UserWithNetworkVO userWithNetworkVO = UserWithNetworkVO.fromUserVO(vo);
        //TODO [rafa] change here to bulk fetch data
        if (user.getNetworks() != null) {
            for (Network network : user.getNetworks()) {
                NetworkVO networkVo = Network.convertNetwork(network);
                userWithNetworkVO.getNetworks().add(networkVo);
            }
        }
        return userWithNetworkVO;
    }

    @Override
    public int deleteById(long id) {
        return createNamedQuery("User.deleteById", of(CacheConfig.bypass()))
                .setParameter("id", id)
                .executeUpdate();
    }

    @Override
    public UserVO find(Long id) {
        User user = find(User.class, id);
        return User.convertToVo(user);
    }

    @Override
    public void persist(UserVO user) {
        User entity = User.convertToEntity(user);
        super.persist(entity);
        user.setId(entity.getId());
    }

    @Override
    public UserVO merge(UserVO existing) {
        User entity = User.convertToEntity(existing);
        User merge = super.merge(entity);
        return User.convertToVo(merge);
    }

    @Override
    public void unassignNetwork(@NotNull UserVO existingUser, @NotNull long networkId) {
        createNamedQuery(Network.class, "Network.findWithUsers", of(CacheConfig.refresh()))
                .setParameter("id", networkId)
                .getResultList()
                .stream().findFirst()
                .ifPresent(existingNetwork -> {
                    User usr = new User();
                    usr.setId(existingUser.getId());
                    existingNetwork.getUsers().remove(usr);
                    merge(existingNetwork);
                });
    }

    @Override
    public List<UserVO> getList(String login, String loginPattern,
                              Integer role, Integer status,
                              String sortField, Boolean sortOrderAsc,
                              Integer take, Integer skip) {
        CriteriaBuilder cb = criteriaBuilder();
        CriteriaQuery<User> cq = cb.createQuery(User.class);
        Root<User> from = cq.from(User.class);

        Predicate[] predicates = CriteriaHelper.userListPredicates(cb, from, ofNullable(login), ofNullable(loginPattern), ofNullable(role), ofNullable(status));
        cq.where(predicates);
        CriteriaHelper.order(cb, cq, from, ofNullable(sortField), Boolean.TRUE.equals(sortOrderAsc));

        TypedQuery<User> query = createQuery(cq);
        cacheQuery(query, of(CacheConfig.refresh()));
        ofNullable(take).ifPresent(query::setMaxResults);
        ofNullable(skip).ifPresent(query::setFirstResult);
        return query.getResultList().stream().map(User::convertToVo).collect(Collectors.toList());
    }

    private Optional<UserVO> optionalUserConvertToVo(Optional<User> login) {
        if (login.isPresent()) {
            return Optional.ofNullable(User.convertToVo(login.get()));
        }
        return Optional.empty();
    }

}
