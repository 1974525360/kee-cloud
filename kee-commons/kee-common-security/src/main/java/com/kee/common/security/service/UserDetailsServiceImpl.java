package com.kee.common.security.service;


import com.kee.api.system.RemoteUserService;
import com.kee.api.system.domain.SysUser;
import com.kee.api.system.model.UserInfo;
import com.kee.common.core.constant.Constants;
import com.kee.common.core.domain.R;
import com.kee.common.core.enums.UserStatus;
import com.kee.common.core.exception.BaseException;
import com.kee.common.core.utils.StringUtils;
import com.kee.common.redis.service.RedisService;
import com.kee.common.security.domain.LoginUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * 用户信息处理
 *
 * @author zms
 */
@Service
public class UserDetailsServiceImpl implements UserDetailsService {
    private static final Logger log = LoggerFactory.getLogger(UserDetailsServiceImpl.class);

    @Autowired
    private RemoteUserService remoteUserService;

    @Autowired
    private RedisService redisService;


    @Override
    public UserDetails loadUserByUsername(String username) {
        R<UserInfo> userResult = remoteUserService.getUserInfo(username);
        checkUser(userResult, username);
        return getUserDetails(userResult);
    }

    public void checkUser(R<UserInfo> userResult, String username) {
        if (StringUtils.isNull(userResult) || StringUtils.isNull(userResult.getData())) {
            log.info("登录用户：{} 不存在.", username);
            throw new UsernameNotFoundException("登录用户：" + username + " 不存在");
        } else if (UserStatus.DELETED.getCode().equals(userResult.getData().getSysUser().getDelFlag())) {
            log.info("登录用户：{} 已被删除.", username);
            throw new BaseException("对不起，您的账号：" + username + " 已被删除");
        } else if (UserStatus.DISABLE.getCode().equals(userResult.getData().getSysUser().getStatus())) {
            log.info("登录用户：{} 已被停用.", username);
            throw new BaseException("对不起，您的账号：" + username + " 已停用");
        }
        //判断是否开启密码错误验证
        if (checkPasswordErrorStatus()) {
            //获取redis缓存信息,判断用户是否被锁定
            validate(username);
        }
    }

    private UserDetails getUserDetails(R<UserInfo> result) {
        UserInfo info = result.getData();
        Set<String> dbAuthsSet = new HashSet<>();
        if (StringUtils.isNotEmpty(info.getRoles())) {
            // 获取角色
            dbAuthsSet.addAll(info.getRoles());
            // 获取权限
            dbAuthsSet.addAll(info.getPermissions());
        }

        Collection<? extends GrantedAuthority> authorities = AuthorityUtils
                .createAuthorityList(dbAuthsSet.toArray(new String[0]));
        SysUser user = info.getSysUser();
        return new LoginUser(user.getUserId(), user.getDeptId(), user.getUserName(), user.getPassword(), true, true, true, true,
                authorities);
    }

    private void validate(String username) {
        String key = Constants.PWD_ERR_CNT_KEY + username;
        Integer retryCount = redisService.getCacheObject(key);
        if (retryCount != null && retryCount >= Constants.PASSWORD_MAX_RETRY_COUNT) {
            log.info("登录用户：{} 已被锁定.", username);
            throw new BaseException("对不起，您的账号：" + username + " 已被锁定");
        }
    }

    private boolean checkPasswordErrorStatus() {
        String key = Constants.SYS_CONFIG_KEY + Constants.PWD_ERR_CONFIG_KEY;
        String pwdConfigValue = redisService.getCacheObject(key);
        return Constants.PWD_ERR_CNT_OPEN.equals(pwdConfigValue);
    }

}