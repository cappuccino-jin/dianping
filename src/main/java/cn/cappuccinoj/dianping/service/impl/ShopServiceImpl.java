package cn.cappuccinoj.dianping.service.impl;

import cn.cappuccinoj.dianping.common.BusinessException;
import cn.cappuccinoj.dianping.common.EmBusinessError;
import cn.cappuccinoj.dianping.dao.ShopModelMapper;
import cn.cappuccinoj.dianping.model.CategoryModel;
import cn.cappuccinoj.dianping.model.SellerModel;
import cn.cappuccinoj.dianping.model.ShopModel;
import cn.cappuccinoj.dianping.service.CategoryService;
import cn.cappuccinoj.dianping.service.SellerService;
import cn.cappuccinoj.dianping.service.ShopService;
import com.alibaba.druid.util.StringUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class ShopServiceImpl implements ShopService {

    @Autowired
    private ShopModelMapper shopModelMapper;

    @Autowired
    private RestHighLevelClient highLevelClient;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private SellerService sellerService;

    private final String INDEX_NAME = "shop";

    @Override
    @Transactional
    public ShopModel create(ShopModel shopModel) throws BusinessException {
        shopModel.setCreatedAt(new Date());
        shopModel.setUpdatedAt(new Date());

        //校验商家是否存在正确
        SellerModel sellerModel = sellerService.get(shopModel.getSellerId());
        if(sellerModel == null){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"商户不存在");
        }

        if(sellerModel.getDisabledFlag() == 1){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"商户已禁用");
        }

        //校验类目
        CategoryModel categoryModel = categoryService.get(shopModel.getCategoryId());
        if(categoryModel == null){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"类目不存在");
        }
        shopModelMapper.insertSelective(shopModel);

        return get(shopModel.getId());
    }

    @Override
    public ShopModel get(Integer id) {
        ShopModel shopModel = shopModelMapper.selectByPrimaryKey(id);
        if(shopModel == null){
            return null;
        }
        shopModel.setSellerModel(sellerService.get(shopModel.getSellerId()));
        shopModel.setCategoryModel(categoryService.get(shopModel.getCategoryId()));
        return shopModel;
    }

    @Override
    public List<ShopModel> selectAll() {
        List<ShopModel> shopModelList = shopModelMapper.selectAll();
        shopModelList.forEach(shopModel -> {
            shopModel.setSellerModel(sellerService.get(shopModel.getSellerId()));
            shopModel.setCategoryModel(categoryService.get(shopModel.getCategoryId()));
        });
        return shopModelList;
    }

    @Override
    public List<ShopModel> recommend(BigDecimal longitude, BigDecimal latitude) {
        List<ShopModel> shopModelList = shopModelMapper.recommend(longitude, latitude);
        shopModelList.forEach(shopModel -> {
            shopModel.setSellerModel(sellerService.get(shopModel.getSellerId()));
            shopModel.setCategoryModel(categoryService.get(shopModel.getCategoryId()));
        });
        return shopModelList;
    }

    @Override
    public List<Map<String, Object>> searchGroupByTags(String keyword, Integer categoryId, String tags) {
        return shopModelMapper.searchGroupByTags(keyword,categoryId,tags);
    }

    @Override
    public Integer countAllShop() {
        return shopModelMapper.countAllShop();
    }

    @Override
    public List<ShopModel> search(BigDecimal longitude,
                                  BigDecimal latitude, String keyword,Integer orderby,
                                  Integer categoryId,String tags) {
        List<ShopModel> shopModelList = shopModelMapper.search(longitude,latitude,keyword,orderby,categoryId,tags);
        shopModelList.forEach(shopModel -> {
            shopModel.setSellerModel(sellerService.get(shopModel.getSellerId()));
            shopModel.setCategoryModel(categoryService.get(shopModel.getCategoryId()));
        });
        return shopModelList;
    }

    @Override
    public Map<String, Object> searchByEsHighLevel(BigDecimal longitude, BigDecimal latitude, String keyword, Integer orderby, Integer categoryId, String tags) throws IOException {

        //SearchRequest searchRequest = new SearchRequest("shop");
        //SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        //sourceBuilder.query(QueryBuilders.matchQuery("name", keyword));
        //// Elastic Search 是存储服务不能一直等下去
        //sourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));
        //searchRequest.source(sourceBuilder);
        //
        //List<Integer> shopIdList = new ArrayList<>();
        //SearchResponse searchResponse = highLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        //SearchHit[] hits = searchResponse.getHits().getHits();
        //for (SearchHit hit : hits) {
        //    shopIdList.add(new Integer(hit.getSourceAsMap().get("id").toString()));
        //}

        Request request = new Request(HttpGet.METHOD_NAME,String.format("/%s/_search", INDEX_NAME));

        //使用低级Client实现复杂查询 面向对象的方法构造Json
        JSONObject queryObj = buildQueryShopObj(longitude, latitude, keyword, orderby, categoryId, tags);

        String reqJson = queryObj.toJSONString();

        System.out.println("reqJson = " + reqJson);
        request.setJsonEntity(reqJson);
        Response response = highLevelClient.getLowLevelClient().performRequest(request);
        String responseStr = EntityUtils.toString(response.getEntity());
        System.out.println("response = " + response);
        JSONObject jsonObject = JSONObject.parseObject(responseStr);
        JSONArray jsonArr = jsonObject.getJSONObject("hits").getJSONArray("hits");
        List<ShopModel> shopModelList = new ArrayList<>();
        for (int i = 0; i < jsonArr.size(); i++) {
            JSONObject jsonObj = jsonArr.getJSONObject(i);
            Integer id = new Integer(jsonObj.get("_id").toString());
            BigDecimal distance = new BigDecimal(jsonObj.getJSONObject("fields").getJSONArray("distance").get(0).toString());
            ShopModel shopModel = get(id);
            shopModel.setDistance(distance.multiply(new BigDecimal(1000).setScale(0, RoundingMode.CEILING)).intValue());
            shopModelList.add(shopModel);
        }

        JSONArray tagsJsonArray = jsonObject.getJSONObject("aggregations").getJSONObject("group_by_tags").getJSONArray("buckets");
        List<Map<String, Object>> tagsList = new ArrayList<>();
        for (int i = 0; i < tagsJsonArray.size(); i++) {
            JSONObject jsonObj = tagsJsonArray.getJSONObject(i);
            Map<String, Object> tagMap = new HashMap<>();
            tagMap.put("tags", jsonObj.getString("key"));
            tagMap.put("num", jsonObj.getString("doc_count"));
            tagsList.add(tagMap);
        }

        HashMap<String, Object> result = new HashMap<>();
        result.put("tags", tagsList);
        result.put("shop", shopModelList);

        return result;
    }

    private JSONObject buildQueryShopObj(BigDecimal longitude, BigDecimal latitude, String keyword, Integer orderby, Integer categoryId, String tags) throws IOException {
        // 构建请求
        JSONObject queryObj = new JSONObject();
        // 构建 source 部分
        queryObj.put("_source", "*");

        // 构建自定义距离字段 script_fields
        JSONObject scriptFields = new JSONObject();
        scriptFields.put("distance", new JSONObject());
        scriptFields.getJSONObject("distance").put("script", new JSONObject());
        scriptFields.getJSONObject("distance").getJSONObject("script").put("source", "haversin(lat,lon,doc['location'].lat,doc['location'].lon)");
        scriptFields.getJSONObject("distance").getJSONObject("script").put("lang", "expression");
        scriptFields.getJSONObject("distance").getJSONObject("script").put("params", new JSONObject());
        scriptFields.getJSONObject("distance").getJSONObject("script").getJSONObject("params").put("lat", latitude);
        scriptFields.getJSONObject("distance").getJSONObject("script").getJSONObject("params").put("lon", longitude);

        queryObj.put("script_fields", scriptFields);

        // 构建 query
        Map<String, Object> cixingMap = analyzeCategoryKeyword(keyword);
        boolean isAffectFilter = false;
        boolean isAffectOrder = true;

        JSONObject query = new JSONObject();

        // 构建 function score
        query.put("function_score", new JSONObject());
        query.getJSONObject("function_score").put("query", new JSONObject());
        query.getJSONObject("function_score").getJSONObject("query").put("bool", new JSONObject());

        //must 召回条件
        final JSONArray mustArray = new JSONArray();
        if (cixingMap.size() > 0 && isAffectFilter) {
            JSONObject boolObj = new JSONObject();
            JSONArray shouldObj = new JSONArray();
            boolObj.put("bool", new JSONObject());
            boolObj.getJSONObject("bool").put("should", shouldObj);

            // 影响召回策略
            for (String key : cixingMap.keySet()) {
                Integer cixingCategoryId = Integer.valueOf(cixingMap.get(key).toString());
                JSONObject matchObj = new JSONObject();
                matchObj.put("match", new JSONObject());
                matchObj.getJSONObject("match").put("name", new JSONObject());
                matchObj.getJSONObject("match").getJSONObject("name").put("query", key);
                matchObj.getJSONObject("match").getJSONObject("name").put("boost", 0.1);
                shouldObj.add(matchObj);

                JSONObject termObj = new JSONObject();
                termObj.put("term", new JSONObject());
                termObj.getJSONObject("term").put("category_id", new JSONObject());
                termObj.getJSONObject("term").getJSONObject("category_id").put("value", cixingCategoryId);
                termObj.getJSONObject("term").getJSONObject("category_id").put("boost", 0.1);
                shouldObj.add(termObj);
            }

            mustArray.add(boolObj);
        } else {
            //1.name
            JSONObject nameObj = new JSONObject();
            nameObj.put("match", new JSONObject());
            nameObj.getJSONObject("match").put("name", new JSONObject());
            nameObj.getJSONObject("match").getJSONObject("name").put("query", keyword);
            nameObj.getJSONObject("match").getJSONObject("name").put("boost", 0.1);
            mustArray.add(nameObj);
        }

        //2.term
        JSONObject termObj = new JSONObject();
        termObj.put("term", new JSONObject());
        termObj.getJSONObject("term").put("seller_disabled_flag", 0);
        mustArray.add(termObj);

        //3.category
        if (categoryId != null) {
            //若用户选中了类目
            final JSONObject category = new JSONObject();
            category.put("term", new JSONObject());
            category.getJSONObject("term").put("category_id", categoryId);
            mustArray.add(category);
        }

        //4.tags
        if (tags != null) {
            //若用户选中了标签
            final JSONObject tag = new JSONObject();
            tag.put("term", new JSONObject());
            tag.getJSONObject("term").put("tags", tags);
            mustArray.add(tag);
        }

        // 构建 function_score 部分
        query.getJSONObject("function_score").getJSONObject("query").getJSONObject("bool").put("must", mustArray);
        JSONArray functions = new JSONArray();

        //functions
        if (orderby == null) {
            //1.gauss
            JSONObject gauss = new JSONObject();
            gauss.put("gauss", new JSONObject());
            gauss.getJSONObject("gauss").put("location", new JSONObject());
            gauss.getJSONObject("gauss").getJSONObject("location").put("origin", latitude + "," + longitude);
            gauss.getJSONObject("gauss").getJSONObject("location").put("scale", "100km");
            gauss.getJSONObject("gauss").getJSONObject("location").put("offset", "0km");
            gauss.getJSONObject("gauss").getJSONObject("location").put("decay", "0.5");
            gauss.put("weight", 9);
            functions.add(gauss);

            if(cixingMap.keySet().size() > 0 && isAffectOrder){
                for (String key : cixingMap.keySet()) {
                    Integer orderCategoryId = Integer.valueOf(cixingMap.get(key).toString());
                    JSONObject filterObj = new JSONObject();
                    filterObj.put("filter", new JSONObject());
                    filterObj.getJSONObject("filter").put("term", new JSONObject());
                    filterObj.getJSONObject("filter").getJSONObject("term").put("category_id", orderCategoryId);
                    filterObj.put("weight", 4);
                    functions.add(filterObj);
                }
            }

        } else {
            //低价排序
            JSONObject orderBy = new JSONObject();
            orderBy.put("field_value_factor", new JSONObject());
            orderBy.getJSONObject("field_value_factor").put("field", "price_per_man");
            orderBy.put("weight", 1);
            functions.add(orderBy);
        }

        //2.remark_sorce
        JSONObject remarkSorce = new JSONObject();
        remarkSorce.put("field_value_factor", new JSONObject());
        remarkSorce.getJSONObject("field_value_factor").put("field", "remark_score");
        remarkSorce.put("weight", 0.2);
        functions.add(remarkSorce);

        //3.seller_remark_score
        JSONObject sellerRemarkScore = new JSONObject();
        sellerRemarkScore.put("field_value_factor", new JSONObject());
        sellerRemarkScore.getJSONObject("field_value_factor").put("field", "seller_remark_score");
        sellerRemarkScore.put("weight", 0.1);
        functions.add(sellerRemarkScore);

        query.getJSONObject("function_score").put("functions", functions);

        //mode
        query.getJSONObject("function_score").put("score_mode", "sum");
        query.getJSONObject("function_score").put("boost_mode", orderby == null ? "sum" : "replace");

        //put query
        queryObj.put("query", query);

        // 构建排序字段 sort
        JSONObject sortObject = new JSONObject();
        JSONArray sortArray = new JSONArray();
        JSONObject scoreObj = new JSONObject();
        scoreObj.put("_score", new JSONObject());
        if (orderby == null) {
            scoreObj.getJSONObject("_score").put("order", "desc");
        } else {
            scoreObj.getJSONObject("_score").put("order", "asc");
        }
        sortArray.add(scoreObj);
        sortObject.put("sort", sortArray);
        // put sort
        queryObj.put("sort", sortArray);

        // 加入使用标签聚合 aggs
        queryObj.put("aggs", new JSONObject());
        queryObj.getJSONObject("aggs").put("group_by_tags", new JSONObject());
        queryObj.getJSONObject("aggs").getJSONObject("group_by_tags").put("terms", new JSONObject());
        queryObj.getJSONObject("aggs").getJSONObject("group_by_tags").getJSONObject("terms").put("field", "tags");
        return queryObj;
    }

    /**
     * 构造分词识别器 增加搜索相关性
     */
    private Map<String ,Object> analyzeCategoryKeyword(String keyword) throws IOException {
        Map<String, Object> res = new HashMap<>();

        Request request = new Request(HttpGet.METHOD_NAME,String.format("/%s/_analyze", INDEX_NAME));
        request.setJsonEntity("{\n \"field\": \"name\",\n \"text\": \"" + keyword + "\"\n}");
        Response response = highLevelClient.getLowLevelClient().performRequest(request);
        String responseStr = EntityUtils.toString(response.getEntity());
        JSONObject jsonObject = JSONObject.parseObject(responseStr);
        JSONArray jsonArray = jsonObject.getJSONArray("tokens");
        for (int i = 0; i < jsonArray.size(); i++) {
            String token = jsonArray.getJSONObject(i).getString("token");
            Integer categoryId = this.getCategoryIdByToken(token);
            if (Objects.nonNull(categoryId)) {
                res.put(token, categoryId);
            }
        }
        return res;
    }

    private Integer getCategoryIdByToken(String token) {
        for (Integer key : categoryWorkMap.keySet()) {
            List<String> tokenList = categoryWorkMap.get(key);
            if(tokenList.contains(token)){
                return key;
            }
        }
        return null;
    }
    
    private Map<Integer, List<String>> categoryWorkMap = new HashMap<>();

    @PostConstruct
    public void init(){
        categoryWorkMap.put(1, new ArrayList<>());
        categoryWorkMap.put(2, new ArrayList<>());

        categoryWorkMap.get(1).add("吃饭");
        categoryWorkMap.get(1).add("下午茶");


        categoryWorkMap.get(2).add("休息");
        categoryWorkMap.get(2).add("睡觉");
        categoryWorkMap.get(2).add("住宿");
        
    }

}
