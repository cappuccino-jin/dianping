package cn.cappuccinoj.dianping.service;



import cn.cappuccinoj.dianping.common.BusinessException;
import cn.cappuccinoj.dianping.model.CategoryModel;

import java.util.List;

public interface CategoryService {

    CategoryModel create(CategoryModel categoryModel) throws BusinessException;
    CategoryModel get(Integer id);
    List<CategoryModel> selectAll();

    Integer countAllCategory();
}
