package cn.cappuccinoj.dianping.service;

import cn.cappuccinoj.dianping.common.BusinessException;
import cn.cappuccinoj.dianping.model.UserModel;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

/**
 * @Author cappuccino
 * @Date 2022-05-18 22:43
 */
public interface UserService {

    UserModel getUser(Integer id);

    UserModel register(UserModel registerUser) throws BusinessException, UnsupportedEncodingException, NoSuchAlgorithmException;

    UserModel login(String telphone,String password) throws UnsupportedEncodingException, NoSuchAlgorithmException, BusinessException;

    Integer countAllUser();
}
