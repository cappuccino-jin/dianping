package cn.cappuccinoj.dianping.service.impl;

import cn.cappuccinoj.dianping.dao.UserModelMapper;
import cn.cappuccinoj.dianping.model.UserModel;
import cn.cappuccinoj.dianping.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @Author cappuccino
 * @Date 2022-05-18 22:44
 */
@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserModelMapper userModelMapper;


    @Override
    public UserModel getUser(Integer id) {
        return userModelMapper.selectByPrimaryKey(id);
    }

}
