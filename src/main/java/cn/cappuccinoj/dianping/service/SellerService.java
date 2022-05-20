package cn.cappuccinoj.dianping.service;

import cn.cappuccinoj.dianping.common.BusinessException;
import cn.cappuccinoj.dianping.model.SellerModel;

import java.util.List;

public interface SellerService {

    SellerModel create(SellerModel sellerModel);
    SellerModel get(Integer id);
    List<SellerModel> selectAll();
    SellerModel changeStatus(Integer id,Integer disabledFlag) throws BusinessException;

    Integer countAllSeller();

}
