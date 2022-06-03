# 友情提示

> 1. **学习来自**：[慕课网 ](https://www.imooc.com/)- [龙虾三少](http://www.imooc.com/t/7143508)。
> 2. **全套学习教程**：[《ES7+Spark 构建高相关性搜索服务&千人千面推荐系统》](https://coding.imooc.com/class/391.html) 。
> 3. **Cappuccino个人博客地址**：[在线博客地址](https://cappuccinoj.cn/)。

ps: 如有侵权请联系作者进行删除。



# dianping系统架构

![image-20220603124545082](http://images.cappuccinoj.cn/PicGoImg/image-20220603124545082.png)

# 项目介绍

基于大众点评搜索以及推荐业务，使用SpringBoot加mybatis结合前端模板搭建运营后台门店管理功能，借助ElasticSearch的最新版本ES7，完成高相关性进阶搜索服务，并基于spark mllib2.4.4构建个性化千人千面推荐系统。



### 组织结构

```
dianping
├── dianping-canal -- 客户端以及调度索引增量
├── dianping-common -- 工具类及通用代码
├── dianping-config -- 项目配置
├── dianping-controller -- 控制层
├── dianping-dao -- 数据库持久层
├── dianping-model -- 实体模块
├── dianping-request -- 请求参数模块
└── dianping-service -- 逻辑层接口
```



### 开发环境

| 工具          | 版本号 | 下载                                                         |
| ------------- | ------ | ------------------------------------------------------------ |
| JDK           | 1.8    | https://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html |
| JDK           | 11     | https://repo.huaweicloud.com/java/jdk/                       |
| Mysql         | 5.7    | https://www.mysql.com/                                       |
| Elasticsearch | 7.7.0  | https://www.elastic.co/downloads/elasticsearch               |
| Logstash      | 7.7.0  | https://www.elastic.co/cn/downloads/logstash                 |
| Kibana        | 7.7.0  | https://www.elastic.co/cn/downloads/kibana                   |
| Canal         | 1.1.5  | https://github.com/alibaba/canal                             |
| Spark MLlib   | 2.4.4  | https://spark.apache.org/docs/latest/mllib-guide.html#data-types-algorithms-and-utilities |



# ES的搜索原理

- 基于Lucene的分布式、高可用、全文检索的搜索引擎

* 独立的网络上的一个或一组进程节点(可以独立部署运行、中间件、支持分布式)
* 对外提供搜索服务(http或transport协议) Elastic Search 7.x 逐渐废弃 transport 协议 
* 对内就是一个搜索数据库



# 名词定义
> 提示：7版本中Type被废弃，索引和类型合并为索引！

| Relational database | ElasticSearch |
| :----: | :----:|
| Database | Index |
| Table | Type |
| Row | Document |
| Column | Field |
| Schema | Mapping |
| Index | Everything is indexed |
| SQL | Query DSL |
| SQL | Query DSL |
| SELECT * FROM table... | GET http://.. |
| UPDATE table SET | PUT http://.. |
| DELETE FROM table ... | DELETE http://.. |
| INSERT INFO ... | PUT http://.. |

## 索引
* 搜索中的数据库或表定义
* 构建文档的时候的缩影创建
## 分词
* 搜索是以词为单位做最基本的搜索单元
* 依靠分词器构建分词
* 用分词器构建倒排索引
* 正向索引（以document为索引的入口）和倒排索引（以分词为索引的入口）
## TF-IDF打分
* TF：token frequency, 分词在document字段（待搜索的字段）中出现的次数
* IDF：inverse document frequency, 逆文档频率，代表分词在整个文档中出现的频率，取反
* TFNORM：token frequency normalized 词频归一化
* BM25：解决词频问题（TF公式的分母）

| 单词ID | 单词 | 文档频率 | 倒排列表(DocID;TF;&#60;POS&#62;)|
| :----: | :----:| :----: | :----:|
| 1 | 谷歌 | 5 | (1;1;<1>),(2;1;<1>),(3;2;<1;6>),(4;1;<1>),(5;1;<1>)|
| 2 | 地图 | 5 | (1;1;<2>),(2;1;<2>),(3;1;<2>),(4;1;<2>),(5;1;<2>)|
| 3 | 之父 | 4 | (1;1;<3>),(2;1;<3>),(4;1;<3>),(5;1;<3>)|
| 4 | 跳槽 | 2 | (1;1;<4>),(4;1;<4>)|
| 5 | Facebook | 5 | (1;1;<5>),(2;1;<5>),(3;1;<8>),(4;1;<5>),(5;1;<8>)|
| 6 | 加盟 | 3 | (2;1;<4>),(3;1;<7>),(5;1;<5>)|
| 7 | 创始人 | 1 | (3;1;<3>)|
| 8 | 拉斯 | 2 | (3;1;<4>),(5;1;<4>)|
| 9 | 离开 | 1 | (3;1;<5>)|
| 10 | 与 | 1 | (4;1;<6>)|



# 分布式原理

* 分片 主从 路由
* 负载均衡和读写分离
* 主分片和副本数



# 分布式部署

> 配置文件 elasticsearch.yml

```
#集群名称
cluster.name: dianping-app
#节点名称
node.name: node-1
#绑定IP地址
network.host: 127.0.0.1
#http监听端口
http.port: 9200
# 如果是使用阿里云、华为云、腾讯云等进行集群配置可以了解一下这个配置
# network.publish_host: 116.xxx.225.xxx
#集群之间的通信端口
transport.tcp.port: 9300
#允许跨域
http.cors.enabled: true
http.cors.allow-origin: "*"
#发现集群节点
discovery.seed_hosts: ["127.0.0.1:9300", "127.0.0.1:9301", "127.0.0.1:9302"]
#是否有资格竞选主节点
cluster.initial_master_nodes: ["127.0.0.1:9300", "127.0.0.1:9301", "127.0.0.1:9302"]
```


# ES语法

```
DELETE /test

PUT /test
{
  "settings": {
    "number_of_shards": 1,
    "number_of_replicas": 2
  }
}

DELETE /employee

//非结构化方式新建索引

PUT /employee
{
  "settings": {
    "number_of_shards": 1,
    "number_of_replicas": 1
  }
}

PUT /employee/_doc/1
{
  "name":"凯杰2",
  "age":30
}

PUT /employee/_doc/1
{
  "name":"凯杰3",
  "age":30
}

PUT /employee/_doc/1
{
  "name":"凯杰3"
}

//获取索引记录

GET /employee/_doc/1

//指定字段修改

POST /employee/_update/1
{
    "doc":{
      "name":"凯杰4",
      "age":30
    }
}

//强制指定创建，若已存在， 则失败
POST /employee/_create/5
{
  "name":"兄弟2",
  "age":29
}

//删除某个文档
DELETE /employee/_doc/2

//查询全部文档
GET /employee/_search

//使用结构化的方式创建索引

PUT /employee
{
  "settings": {
    "number_of_replicas": 1,
    "number_of_shards": 1
  },
  "mappings": {
    "properties": {
      "name":{
        "type":"text"
      },
      "age":{
        "type":"integer"
      }
    }
  }
}


//不带条件查询所有记录
GET /employee/_search
{
  "query":{
    "match_all": {
      
    }
  }
}


//分页查询
GET /employee/_search
{
  "query":{
    "match_all": {
      
    }
  },
  "from":0,
  "size":2
}


//带关键字条件的查询
GET /employee/_search
{
  "query":{
    "match":{
      "name":"cappucicno"
    }
  }
}

// 排序

GET /employee/_search
{
  "query":{
    "match_all": {}
  },
  "sort":[{
    "age":{
      "order":"desc"}
  }
  ]
}

//带filter
GET /employee/_search
{
  "query":{
    "bool": {
      "filter": [
        {"match": {
          "name": "兄"
        }}
      ]
    }
  }
}

//带聚合
GET /employee/_search
{
  "query": {"match": {
    "name": "兄"
  }},
  "sort":[
    {"age":{"order":"desc"}}
  ]
  , "aggs": {
    "group_by_age": {
      "terms": {
        "field": "age",
        "size": 10
      }
    }
  }
}

//新建一个索引
PUT /movie/_doc/1
{
  "name":"Eating an apple a day & keeps the doctor away"
}


GET /movie/_search
{
  "query": {
    "match": {
      "name": "awai"
    }
  }
}

//使用analyze api 查看分词状态
GET /movie/_analyze
{
  "field": "name",
  "text":"Eating an apple a day & keeps the doctor away"
}

GET /movie/_analyze
{
  "field": "name",
  "text":"eat"
}

DELETE /movie

//使用结构化的方式重新创建索引
PUT /movie
{
  "settings": {
    "number_of_shards": 1,
    "number_of_replicas": 1
  },
  "mappings": {
    "properties": {
      "name":{
        "type": "text"
        , "analyzer": "english"
      }
    }
  }
}

GET /movie/_search
{
 
}

//开始玩转tmdb
PUT /movie
{
  "settings": {
    "number_of_shards": 1,
    "number_of_replicas": 1
  },
  "mappings": {
    "properties": {
      "title":{
        "type": "text",
        "analyzer": "english"
      },
      "tagline":{
          "type":"text",
          "analyzer": "english"
      },
      "release_date":{
        "type":"date","format":"8yyyy/MM/dd||yyyy/M/dd||yyyy/MM/d||yyyy/M/d"
      },
      "popularity":{
        "type":"double"
      },
      "overview":{
        "type":"text",
        "analyzer": "english"
      },
      "cast":{
        "type":"object",
        "properties": {
          "character":{
            "type":"text",
            "analyzer":"standard"
          },
          "name":{
            "type":"text",
            "analyzer":"standard"
          }
        }
      }
    }
  }
}

# 搜索内容 match

GET /movie/_search
{
  "query":{
    "match": {
      "title": "steve zissou"
    }
  }
}

GET /movie/_analyze
{
  "field": "title",
  "text":"basketball with cartoom aliens"
}

#term 查询

GET /movie/_search
{
  "query":{
    "term": {
      "title": {
        "value": "steve jobs"
      }
    }
  }
}

#分词后的and和or的逻辑，match默认使用的是or
GET /movie/_search
{
  "query":{
    "match": {
      "title": "basketball with cartoom aliens"
    }
  }
}


#改成 and
GET /movie/_search
{
  "query":{
    "match": {
      "title": {
        "query": "basketball with cartoom aliens",
        "operator":"and"
      }
    }
  }
}

#最小词匹配项
GET /movie/_search
{
  "query":{
    "match": {
      "title": {
        "query": "basketball Love aliens",
        "operator":"or",
        "minimum_should_match": 2
      }
    }
  }
}

#短语匹配
GET /movie/_search
{
  "query":{
    "match_phrase": {
      "title": "steve zissou"
    }
  }
}

#多字段查询
GET /movie/_search
{
  "explain":true, 
  "query":{
    "multi_match": {
      "query": "basketball with cartoom aliens",
      "fields": ["title", "overview"]
    }
  }
}

#优化多字段查询
GET /movie/_search
{
  "explain":true, 
  "query":{
    "multi_match":{
      "query": "basketball with cartoom aliens",
      "fields": ["title^10", "overview"],
      "tie_breaker": 0.3
    }
  }
}

#bool查询
#must:必须都为true
#must not:必须都是false
#should:其中只有一个为true即可
#为true的越多则得分越高
GET /movie/_search
{
  "query": {
    "bool":{
      "should": [
        {
          "match": {
            "title": "basketball with cartoom aliens"
          }
        },
        {
          "match": {
            "overview": "basketball with cartoom aliens"
          }
        }
      ]
    }
  }
}

#不同的multi_query其实是有不同的type
#best_fields:默认的得分方式，取得最高的分数作为对应文档的对应分数，“最匹配模式” dis_max
GET /movie/_search
{
  "explain":true,
  "query":{
    "multi_match": {
      "query": "basketball with cartoom aliens",
      "fields": ["title","overview"],
      "type": "best_fields"
    }
  }
}

GET /movie/_search
{
  "explain":true,
  "query":{
    "dis_max": {
      "queries": [
        {
          "match": {
            "title": "basketball with cartoom aliens"
          }
        },
        {
          "match": {
            "pverview": "basketball with cartoom aliens"
          }
        }
      ]
    }
  }
}

GET /movie/_validate/query?explain
{
  "query":{
    "multi_match": {
      "query":"basketball with cartoom aliens",
      "fields": ["title^10","overview"],
      "type":"best_fields"
    }
  }
}

#most_fields:考虑绝大多数（所有的）文档的字段得分相加，获得我们需要的结果
GET /movie/_validate/query?explain
{
  "query":{
    "multi_match": {
      "query": "basketball with cartoom aliens",
      "fields": ["title^10","overview^0.1"],
      "type": "most_fields"
    }
  }
}

#cross_fields: 以分词为单位计算栏位的总分,适用于词导向的匹配
GET /movie/_search
{
  "explain":true,
  "query":{
    "multi_match": {
      "query": "steve job",
      "fields": ["title", "overview"],
      "type": "cross_fields"
    }
  }
}

GET /movie/_validate/query?explain
{
  "query":{
    "multi_match": {
      "query": "steve job",
      "fields": ["title", "overview"],
      "type": "cross_fields"
    }
  }
}

#query string
#方便的利用 AND OR NOT
GET /movie/_search
{
  "query":{
    "query_string": {
      "fields": ["title"],
      "query": "steve AND jobs"
    }
  }
}

#filter过滤查询
#单条件过滤
GET /movie/_search
{
  "query":{
    "bool": {
      "filter":{
        "term": {
          "title": "steve"
        }
      }
    }
  }
}

#多条件过滤
GET /movie/_search
{
  "query": {
    "bool": {
      "filter":[
        {
          "term": {
            "title": "steve"
          }
        },
        {
          "term": {
            "cast.name": "gaspard"
          }
        },
        {
          "range": {
            "release_date": {
              "lte": "2015/01/01"
            }
          }
        },
        {
          "range": {
            "popularity": {
              "gte": "25"
            }
          }
        }
      ]
    }
  },
  "sort":[
    {
      "popularity": {"order":"desc"}
    }
  ]
}

#带match打分的filter
GET /movie/_search
{
  "query": {
    "bool": {
      "should": [
        {
          "match": {
            "title": "life"
          }
        }
      ], 
      "filter":[
        {
          "term": {
            "title": "steve"
          }
        },
        {
          "term": {
            "cast.name": "gaspard"
          }
        },
        {
          "range": {
            "release_date": {
              "lte": "2015/01/01"
            }
          }
        },
        {
          "range": {
            "popularity": {
              "gte": "25"
            }
          }
        }
      ]
    }
  }
}

#functionscore
GET /movie/_search
{
  "explain": true, 
  "query": {
    "function_score": {
      //原始查询得到的oldScore
      "query":{
        "multi_match": {
          "query": "steve job",
          "fields": ["title", "overview"],
          "operator": "or",
          "type": "most_fields"
        }
      },
      "functions": [
        {
          "field_value_factor": {
            "field": "popularity",  //对应要调整处理的字段
            "modifier": "log2p",
            "factor": 10
          }
        },
        {
          "field_value_factor": {
            "field": "popularity",  //对应要调整处理的字段
            "modifier": "log2p",
            "factor": 5
          }
        }
      ],
      "score_mode": "sum",  //不同的field value之间得分相加
      "boost_mode": "sum"  //最后再与old value相加
    }
  }
}


GET /shop/_search
{
  "query": {
    "match": {
      "name": "凯悦"
    }
  }
}

GET /shop/_analyze
{
  "analyzer": "ik_smart",
  "text": "凯悦"
}

GET /shop/_analyze
{
  "analyzer": "ik_max_word",
  "text": "花悦庭果木烤鸭"
}

#带上距离字段
GET /shop/_search
{
  "query": {
    "match": {
      "name": "凯悦"
    }
  },

  "script_fields": {
    "distance": {
      "script": {
        "source": "haversin(lat,lon,doc['location'].lat,doc['location'].lon)",
        "lang": "expression",
        "params": {"lat":31.37,"lon":127.12}
      }
    }
  }
}

#使用距离排序
GET /shop/_search
{
  "query": {
    "match": {
      "name": "凯悦"
    }
  },
  "_source": "*", 
  "script_fields": {
    "distance": {
      "script": {
        "source": "haversin(lat,lon,doc['location'].lat,doc['location'].lon)",
        "lang": "expression",
        "params": {"lat":31.37,"lon":127.12}
      }
    }
  },
  "sort":[
    {
      "_geo_distance":{
        "location":{
          "lat":31.37,
          "lon":127.12
        },
        "order":"asc",
        "unit":"km",
        "distance_type":"arc"
      }
    }
  ]
}

#使用funcation score解决排序模型
GET /shop/_search
{
  "explain": true, 
  "_source": "*",
   "script_fields": {
    "distance": {
      "script": {
        "source": "haversin(lat,lon,doc['location'].lat,doc['location'].lon)",
        "lang": "expression",
        "params": {"lat":31.23916171,"lon":121.48789949}
      }
    }
  },
  "query": {
    "function_score": {
      "query": {
        "bool": {
          "must": [
            {"match": {"name": {"query": "凯悦"}}},
            {"term": {"seller_disabled_flag": "0"}}
          ]
        }
      },
      "functions": [
        {
          "gauss": {
            "location": {
              "origin": "31.23916171,121.48789949",
              "scale": "100km",
              "offset": "0km",
              "decay": 0.5
            }
          },
          "weight": 9
        },
        {
          "field_value_factor": {
            "field": "remark_score"
          },
          "weight": 0.2
        },
        {
          "field_value_factor": {
            "field": "seller_remark_score"
          },
          "weight": 0.2
        }
      ],
      "score_mode": "sum",
      "boost_mode": "sum"
    }
  },
  "sort": [
    {
      "_score": {
        "order": "desc"
      }
    }
  ]
}

#使用function score解决排序模型 (高斯衰减函数打分)
#加入分类过滤
GET /shop/_search
{
  "_source": "*",
  "script_fields": {
    "distance": {
      "script": {
        "source": "haversin(lat,lon,doc['location'].lat,doc['location'].lon)",
        "lang": "expression",
        "params": {
          "lat": 31.23916171,
          "lon": 121.48789949
        }
      }
    }
  },
  "query": {
    "function_score": {
      "query": {
        "bool": {
          "must": [
            {
              "match": {
                "name": {
                  "query": "凯悦",
                  "boost": 0.1
                }
              }
            },
            {
              "term": {
                "seller_disabled_flag": 0
              }
            }
            ,
            {
              "term": {
                "category_id": 2
              }
            }
          ]
        }
      },
      "functions": [
        {
          "gauss": {
            "location": {
              "origin": "31.23916171,121.48789949",
              "scale": "100km",
              "offset": "0km",
              "decay": 0.5
            }
          },
          "weight": 9
        },
        {
          "field_value_factor": {
            "field": "remark_score"
          },
          "weight": 0.2
        },
        {
          "field_value_factor": {
            "field": "seller_remark_score"
          },
          "weight": 0.1
        }
      ],
      "score_mode": "sum",
      "boost_mode": "sum"
    }
  },
  "sort": [
    {
      "_score": {
        "order": "desc"
      }
    }
  ]
}


# 低价排序模型
GET /shop/_search
{
  "_source": "*",
  "script_fields": {
    "distance": {
      "script": {
        "source": "haversin(lat,lon,doc['location'].lat,doc['location'].lon)",
        "lang": "expression",
        "params": {
          "lat": 31.23916171,
          "lon": 121.48789949
        }
      }
    }
  },
  "query": {
    "function_score": {
      "query": {
        "bool": {
          "must": [
            {
              "match": {
                "name": {
                  "query": "凯悦",
                  "boost": 0.1
                }
              }
            },
            {
              "term": {
                "seller_disabled_flag": 0
              }
            },
            {
              "term": {
                "tags": {
                  "value": "落地大窗"
                }
              }
            }
          ]
        }
      },
      "functions": [
        {
          "field_value_factor": {
            "field": "price_per_man"
          },
          "weight": 1
        }
      ],
      "score_mode": "sum",
      "boost_mode": "replace"
    }
  },
  "sort": [
    {
      "_score": {
        "order": "asc"
      }
    }
  ],
  "aggs": {
    "group_by_tags": {
      "terms": {
        "field": "tags"
      }
    }
  }
}

GET /shop/_search
{
  "query": {
    "match": {
      "name": "凯悦"
    }
  }
}

GET /shop/_analyze
{
  "analyzer": "ik_max_word",
  "text": "凯悦酒店"
}

# 通过query的方式更新索引
POST /shop/_update_by_query
{
  "query":{
    "bool": {
      "must": [
        {"term": {
          "name": {
            "value": "凯"
          }
        }},
        {"term": {
          "name": {
            "value": "悦"
          }
        }}
      ]
    }
  }
}

#定义支持同义词的门店索引结构
PUT /shop
{
  "settings": {
    "number_of_replicas": 1,
    "number_of_shards": 1,
    "analysis":{
      "filter":{
        "my_synonym_filter":{
          "type":"synonym",
          "synonyms_path":"../plugins/analysis-ik/config/synonyms.txt"
        }
      },
      "analyzer":{
        "ik_syno":{
          "type":"custom",
          "tokenizer":"ik_smart",
          "filter":["my_synonym_filter"]
        },
        "ik_syno_max":{
          "type":"custom",
          "tokenizer":"ik_max_word",
          "filter":["my_synonym_filter"]
        }
      }
    }
  },
  "mappings": {
    "properties": {
      "id":{"type":"integer"},
      "name":{"type":"text","analyzer": "ik_syno_max","search_analyzer":"ik_syno"},
      "tags":{"type": "text","analyzer": "whitespace","fielddata":true},
      "location":{"type":"geo_point"},
      "remark_score":{"type": "double"},
      "proce_per_man":{"type":"integer"},
      "category_id":{"type": "integer"},
      "category_name":{"type": "keyword"},
      "seller_id":{"type": "integer"},
      "seller_remark_score":{"type": "double"},
      "seller_disabled_flag":{"type": "integer"}
    }
  }
}

GET /shop/_analyze
{
  "field": "name",
  "text": "凯悦"
}

GET /shop/_search


GET /shop/_search
{
  "query": {
    "match": {
      "id": 1
    }
  }
}

GET /shop/_search
{
  "query": {
    "term": {
      "category_name": "美食1"
    }
  }
}

#采取词性影响召回策略模型
GET /shop/_search
{
  "_source": "*",
  "script_fields": {
    "distance": {
      "script": {
        "source": "haversin(lat,lon,doc['location'].lat,doc['location'].lon)",
        "lang": "expression",
        "params": {
          "lat": 31.23916171,
          "lon": 121.48789949
        }
      }
    }
  },
  "query": {
    "function_score": {
      "query": {
        "bool": {
          "must": [
            {
              "bool": {
                "should": [
                  {
                    "match": {
                      "name": {
                        "query": "凯悦休息",
                        "boost": 0.1
                      }
                    }
                  }
                ]
              }
            },
            {
              "term": {
                "seller_disabled_flag": 0
              }
            }
          ]
        }
      },
      "functions": [
        {
          "gauss": {
            "location": {
              "origin": "31.23916171,121.48789949",
              "scale": "100km",
              "offset": "0km",
              "decay": 0.5
            }
          },
          "weight": 9
        },
        {
          "filter": {
            "term": {
              "category_id": 2
            }
          },
          "weight": 3
        },
        {
          "field_value_factor": {
            "field": "remark_score"
          },
          "weight": 0.2
        },
        {
          "field_value_factor": {
            "field": "seller_remark_score"
          },
          "weight": 0.1
        }
        
      ],
      "score_mode": "sum",
      "boost_mode": "sum"
    }
  },
  "sort": [
    {
      "_score": {
        "order": "desc"
      }
    }
  ]
}

GET /shop/_search
{
      "script_fields": {
        "distance": {
            "script": {
                "source": "haversin(lat,lon,doc['location'].lat,doc['location'].lon)",
                "lang": "expression",
                "params": {
                    "lon": 121.48789949,
                    "lat": 31.23916171
                }
            }
        }
    },
    "query": {
        "function_score": {
            "functions": [
                {
                    "gauss": {
                        "location": {
                            "offset": "0km",
                            "origin": "31.23916171,121.48789949",
                            "scale": "100km",
                            "decay": "0.5"
                        }
                    },
                    "weight": 100
                },
                {
                    "filter": {
                        "term": {
                            "orderCategoryId": 1
                        }
                    },
                    "weight": 4
                },
                {
                    "field_value_factor": {
                        "field": "remark_score"
                    },
                    "weight": 0.2
                },
                {
                    "field_value_factor": {
                        "field": "seller_remark_score"
                    },
                    "weight": 0.1
                }
            ],
            "score_mode": "sum",
            "query": {
                "bool": {
                    "must": [
                        {
                            "match": {
                                "name": {
                                    "query": "凯悦吃饭",
                                    "boost": 0.1
                                }
                            }
                        },
                        {
                            "term": {
                                "seller_disabled_flag": 0
                            }
                        }
                    ]
                }
            },
            "boost_mode": "sum"
        }
    },

    "_source": "*",
    "sort": [
        {
            "_score": {
                "order": "desc"
            }
        }
    ]
}

#测试ik分词器，只能分词器
GET _analyze?pretty
{
  "analyzer": "ik_smart",
  "text": ["中华人民共和国国歌"]
}

#最大化分词
GET _analyze?pretty
{
  "analyzer": "ik_max_word",
  "text": ["中华人民共和国国歌"]
}

GET _analyze?pretty
{
  "analyzer": "standard",
  "text": ["中华人民共和国国歌"]
}


#analyzer指定的是构建索引的时候的分词
#search_analyzer指定的是搜索关键字的时候的分词

#最佳实践：索引的时候使用max_word,但是查询的时候使用smartword


#定义门店索引结构
PUT /shop
{
  "settings": {
    "number_of_replicas": 1,
    "number_of_shards": 1
  },
  "mappings": {
    "properties": {
      "id":{"type":"integer"},
      "name":{"type":"text","analyzer": "ik_max_word","search_analyzer":"ik_smart"},
      "tags":{"type": "text","analyzer": "whitespace","fielddata":true},
      "location":{"type":"geo_point"},
      "remark_score":{"type": "double"},
      "proce_per_man":{"type":"integer"},
      "category_id":{"type": "integer"},
      "category_name":{"type": "keyword"},
      "seller_id":{"type": "integer"},
      "seller_remark_score":{"type": "double"},
      "seller_disabled_flag":{"type": "integer"}
    }
  }
}
```
* 查全率：正确的结果有n个，查询出来的正确的有m m/n
* 查准率：查出的n个文档有m个正确 m/n
* 两者不可兼得，但是可以调整排序



> analyze分析过程
>
> > standard analyze:分析=分词的过程：字符过滤器(过滤字符)->字符处理(标准字符处理，以空格和标点符号分割内容)->分词过滤(分词转换,变小写)
>
> > english analyze:分析=分词的过程：字符过滤器(过滤特殊符号外加量词，the等等)->字符处理(以空格和标点符号分割内容)->分词过滤(分词转换,词干转化,去除复数)



* 类型

|类型|说明|
|:---:|:---:|
|Text|被分析索引的字符串类型|
|Keyword|不能被分析只能被精确匹配的字符串类型|
|Date|日期类型,可以配合format一起使用|
|long, integer, short, double|数字类型|
|boolean|true false|
|Array|["one","two"]|
|Object|json嵌套|
|Ip|192.168.71.1|
|Geo_point|地理位置|



* IK分词器
* IK Analyze : 字符过滤器(过滤特殊符号，量词，停用词)->基于词库词典进行分词
* https://github.com/medcl/elasticsearch-analysis-ik
* ik_smart:智能分词法
* ik_max_word:最大分词法
* "analyzer": 构建索引的分词
* "search_analyzer": 查询的分词
* 最佳实践： 索引的时候使用max_word，查询的时候用smart
```
# 测试IK分词器
GET _analyze?pretty
{
  "analyzer": "ik_smart",
  "text":"中华人民共和国国歌"
}
GET _analyze?pretty
{
  "analyzer": "standard",
  "text":"中华人民共和国国歌"
}
GET _analyze?pretty
{
  "analyzer": "ik_max_word",
  "text":"中华人民共和国国歌"
}
```
### 门店索引构建
```
PUT /shop
{
  "settings": {
    "number_of_replicas": 1,
    "number_of_shards": 1
  },
  "mappings": {
    "properties": {
      "id":{"type": "integer"},
      "name":{"type": "text","analyzer": "ik_max_word","search_analyzer": "ik_smart"},
      "tags":{"type": "text","analyzer": "whitespace","fielddata": true},
      "location":{"type":"geo_point"},
      "price_per_man":{"type": "integer"},
      "category_id":{"type": "integer"},
      "category_name":{"type": "keyword"},
      "seller_id":{"type": "integer"},
      "seller_remark_score":{"type": "double"},
      "seller_disable_flag":{"type": "integer"}
    }
  }
}
```


# logstash-input-jdbc增量全量的同步

* logstash安装logstash-input-jdbc插件
```
logstash-plugin install logstash-input-jdbc #目前貌似不用执行此命令也可
```
* 编写配置文件jdbc.conf,jdbc.sql

```properties
version1.0=========================================================================

input {
	jdbc {
		# mysql 数据库连接, dianping为数据库名
		jdbc_connection_string => "jdbc:mysql://gz-cdb-r92c8vgn.sql.tencentcdb.com:57859/dianping?useUnicode=true&characterEncoding=UTF-8"
		# 用户名和密码
		jdbc_user => "root"
		jdbc_password => "!qq1224767958"
		# 驱动
		jdbc_driver_library => "/opt/install/logstash-7.7.0/bin/mysql/mysql-connector-java-8.0.25.jar"
		# 驱动类名
		jdbc_driver_class => "com.mysql.cj.jdbc.Driver"
		jdbc_paging_enabled => "true"
		jdbc_page_size => "50000"
		# 执行的 sql 文件路径 + 名称
		statement_filepath =>"/opt/install/logstash-7.7.0/bin/mysql/jdbc.sql"
		# 设置监听间隔 各字段含义 （由左至右）分、时、天、月、年，全部为*默认含义为每分钟都要更新
		schedule => "* * * * *"
	}
}

output {
	elasticsearch {
		# ES 的 IP 地址及端口
		hosts => ["localhost:9200"]
		# 索引名称
		index => "shop"
		document_type => "_doc"
		# 自增 ID 需要关联的数据库中有一个 id 字段, 对应索引的 id 号
		document_id => "%{id}"
	}
	stdout {
		# JSON 格式输出
		codec => json_lines
	}
}


version2.0=========================================================================

input {
	jdbc {
		# 设置 timezone
		jdbc_default_timezone => "Asia/Shanghai"
		# mysql 数据库连接, dianping为数据库名
		jdbc_connection_string => "jdbc:mysql://gz-cdb-r92c8vgn.sql.tencentcdb.com:57859/dianping?useUnicode=true&characterEncoding=UTF-8"
		# 用户名和密码
		jdbc_user => "root"
		jdbc_password => "!qq1224767958"
		# 驱动
		jdbc_driver_library => "/opt/install/logstash-7.7.0/bin/mysql/mysql-connector-java-8.0.25.jar"
		# 驱动类名
		jdbc_driver_class => "com.mysql.cj.jdbc.Driver"
		jdbc_paging_enabled => "true"
		jdbc_page_size => "50000"
		last_run_metadata_path => "/opt/install/logstash-7.7.0/bin/mysql/last_value_meta"
		# 执行的 sql 文件路径 + 名称
		statement_filepath =>"/opt/install/logstash-7.7.0/bin/mysql/jdbc.sql"
		# 设置监听间隔 各字段含义 （由左至右）分、时、天、月、年，全部为*默认含义为每分钟都要更新
		schedule => "* * * * *"
	}
}

output {
	elasticsearch {
		# ES 的 IP 地址及端口
		hosts => ["localhost:9200"]
		# 索引名称
		index => "shop"
		document_type => "_doc"
		# 自增 ID 需要关联的数据库中有一个 id 字段, 对应索引的 id 号
		document_id => "%{id}"
	}
	stdout {
		# JSON 格式输出
		codec => json_lines
	}
}
```

jdbc.sql

```sql
version1.0=========================================================================

SELECT
	a.id,
	a.NAME,
	a.tags,
	concat( a.latitude, ',', a.longitude ) AS location,
	a.remark_score,
	a.price_per_man,
	a.category_id,
	b.NAME AS category_name,
	a.seller_id,
	c.remark_score AS seller_remark_score,
	c.disabled_flag AS seller_disabled_flag 
FROM
	shop a
	INNER JOIN category b ON a.category_id = b.id
	INNER JOIN seller c ON c.id = a.seller_id
	
	
	
version2.0=========================================================================
SELECT
	a.id,
	a.NAME,
	a.tags,
	concat( a.latitude, ',', a.longitude ) AS location,
	a.remark_score,
	a.price_per_man,
	a.category_id,
	b.NAME AS category_name,
	a.seller_id,
	c.remark_score AS seller_remark_score,
	c.disabled_flag AS seller_disabled_flag 
FROM
	shop a
	INNER JOIN category b ON a.category_id = b.id
	INNER JOIN seller c ON c.id = a.seller_id 
WHERE
	a.updated_at > : sql_last_value 
	OR b.updated_at > : sql_last_value 
	OR c.updated_at > : sql_last_value
```



> 启动logstash同步

```shell
logstash -f jdbc.conf
```


> 基于LBS计算距离

```
#带上距离字段查询haversin计算距离expression表达式
GET /shop/_search
{
  "query":{
    "match": {
      "name": "凯悦"
    }
  },
  "_source": "*", 
  "script_fields": {
    "distance": {
      "script": {
        "source": "haversin(lat,lon,doc['location'].lat,doc['location'].lon)",
        "lang":"expression",
        "params": {"lat":31.37,"lon":127.12}
      }
    }
  }
}
#使用距离排序
GET /shop/_search
{
  "query":{
    "match": {
      "name": "凯悦"
    }
  },
  "_source": "*", 
  "script_fields": {
    "distance": {
      "script": {
        "source": "haversin(lat,lon,doc['location'].lat,doc['location'].lon)",
        "lang":"expression",
        "params": {"lat":31.37,"lon":127.12}
      }
    }
  },
  "sort": [
    {
      "_geo_distance": {
        "location": {
          "lat": 31.37,
          "lon": 127.12
        }, 
        "order": "asc",
        "unit": "km",
        "distance_type": "arc"
      }
    }
  ]
}
#使用function score解决排序模型 (高斯衰减函数打分)
GET /shop/_search
{
  "explain": true, 
  "_source": "*",
  "script_fields": {
    "distance": {
      "script": {
        "source": "haversin(lat,lon,doc['location'].lat,doc['location'].lon)",
        "lang": "expression",
        "params": {
          "lat": 31.23916171,
          "lon": 121.48789949
        }
      }
    }
  },
  "query": {
    "function_score": {
      "query": {
        "bool": {
          "must": [
            {
              "match": {
                "name": {
                  "query": "凯悦",
                  "boost": 0.1
                }
              }
            },
            {
              "term": {
                "seller_disabled_flag": 0
              }
            }
          ]
        }
      },
      "functions": [
        {
          "gauss": {
            "location": {
              "origin": "31.23916171,121.48789949",
              "scale": "100km",
              "offset": "0km",
              "decay": 0.5
            }
          },
          "weight": 9
        },
        {
          "field_value_factor": {
            "field": "remark_score"
          },
          "weight": 0.2
        },
        {
          "field_value_factor": {
            "field": "seller_remark_score"
          },
          "weight": 0.1
        }
      ],
      "score_mode": "sum",
      "boost_mode": "sum"
    }
  },
  "sort": [
    {
      "_score": {
        "order": "desc"
      }
    }
  ]
}
```
> gauss算法图

![image-20220528183605905](http://images.cappuccinoj.cn/PicGoImg/image-20220528183605905.png)



* 低价排序
```
GET /shop/_search
{
  "explain": true, 
  "_source": "*",
  "script_fields": {
    "distance": {
      "script": {
        "source": "haversin(lat,lon,doc['location'].lat,doc['location'].lon)",
        "lang": "expression",
        "params": {
          "lat": 31.23916171,
          "lon": 121.48789949
        }
      }
    }
  },
  "query": {
    "function_score": {
      "query": {
        "bool": {
          "must": [
            {
              "match": {
                "name": {
                  "query": "凯悦",
                  "boost": 0.1
                }
              }
            },
            {
              "term": {
                "seller_disabled_flag": 0
              }
            }
          ]
        }
      },
      "functions": [
        {
          "field_value_factor": {
            "field": "price_per_man"
          },
          "weight": 1
        }
      ],
      "score_mode": "sum",
      "boost_mode": "replace"
    }
  },
  "sort": [
    {
      "_score": {
        "order": "asc"
      }
    }
  ]
}
```
* 根据标签聚合
```
GET /shop/_search
{
  "_source": "*",
  "script_fields": {
    "distance": {
      "script": {
        "source": "haversin(lat,lon,doc['location'].lat,doc['location'].lon)",
        "lang": "expression",
        "params": {
          "lat": 31.23916171,
          "lon": 121.48789949
        }
      }
    }
  },
  "query": {
    "function_score": {
      "query": {
        "bool": {
          "must": [
            {
              "match": {
                "name": {
                  "query": "凯悦",
                  "boost": 0.1
                }
              }
            },
            {
              "term": {
                "seller_disabled_flag": 0
              }
            },
            {
              "term": {
                "tags": "落地大窗"
              }
            }
          ]
        }
      },
      "functions": [
        {
          "field_value_factor": {
            "field": "price_per_man"
          },
          "weight": 1
        }
      ],
      "score_mode": "sum",
      "boost_mode": "replace"
    }
  },
  "sort": [
    {
      "_score": {
        "order": "asc"
      }
    }
  ],
  "aggs": {
    "group_by_tags": {
      "terms": {
        "field": "tags"
      }
    }
  }
}
```

### 定制化词库
* 在ik分词器中添加字典，修改配置文件指向该字典，然后重启ES
* 更新索引重新构建分词
```
POST /shop/_update_by_query
{
  "query":{
    "bool":{
      "must":[
        {"term":{"name":"凯"}},
        {"term":{"name":"悦"}}
      ]
    }
  }
}
```
* 热更新词库
```
修改配置文件指向字典
<entry key="ext_dic>http://ssss</entry>
HTTP请求需要返回两个头部，last-modified和etag
两者任何一个发生变化则重新更新，ik一分钟检测一次
```
* 同义词扩展
```
在词典中新建synonyms.txt放入"红色,大红,橘红"等同义词
重建索引自定义同义词分词器
PUT /shop
{
  "settings": {
    "number_of_replicas": 1,
    "number_of_shards": 1,
    "analysis":{
      "filter":{
        "my_synonym_filter":{ //创建一个自定义过滤器，指向同义词文件
          "type":"synonym",
          "synonyms_path":"analysis-ik/synonyms.txt"
        }
      },
      "analyzer":{ //自定义分词器，在最后的时候调用上面的过滤器
        "ik_syno":{
          "type":"custom",
          "tokenizer":"ik_smart",
          "filter":["my_my_synonym_filters"]
        },
        "ik_syno_max":{
          "type":"custom",
          "tokenizer":"ik_max_word",
          "filter":["my_my_synonym_filters"]
        }
      }
    }
  },
  "mappings": {
    "properties": {
      "id":{"type": "integer"},
      "name":{"type": "text","analyzer": "ik_syno_max","search_analyzer": "ik_syno"},
      "tags":{"type": "text","analyzer": "whitespace","fielddata": true},
      "location":{"type":"geo_point"},
      "price_per_man":{"type": "integer"},
      "category_id":{"type": "integer"},
      "category_name":{"type": "keyword"},
      "seller_id":{"type": "integer"},
      "seller_remark_score":{"type": "double"},
      "seller_disable_flag":{"type": "integer"}
    }
  }
}
```
* 相关性重塑
```
GET /shop/_search
{
  "explain": true, 
  "_source": "*",
  "script_fields": {
    "distance": {
      "script": {
        "source": "haversin(lat,lon,doc['location'].lat,doc['location'].lon)",
        "lang": "expression",
        "params": {
          "lat": 31.23916171,
          "lon": 121.48789949
        }
      }
    }
  },
  "query": {
    "function_score": {
      "query": {
        "bool": {
          "must": [
            {
              "bool": {
                "should": [
                  {"match": { "name": {"query": "凯悦", "boost": 0.1}}}, //满足名字搜索
                  {"term": {"category_id": {"value": "2","boost": }}} //或者满足分类不影响排序评分
                ]
              }
            },
            {
              "term": {
                "seller_disabled_flag": 0
              }
            }
          ]
        }
      },
      "functions": [
        {
          "gauss": {
            "location": {
              "origin": "31.23916171,121.48789949",
              "scale": "100km",
              "offset": "0km",
              "decay": 0.5
            }
          },
          "weight": 9
        },
        {
          "field_value_factor": {
            "field": "remark_score"
          },
          "weight": 0.2
        },
        //相关性影响排序
        {
          "field_value_factor": {
            "field": "seller_remark_score"
          },
          "weight": 0.1
        },
        {
          "filter": {"term": {"category_id": "2"}},
          "weight": 0.2
        }
      ],
      "score_mode": "sum",
      "boost_mode": "sum"
    }
  },
  "sort": [
    {
      "_score": {
        "order": "desc"
      }
    }
  ]
}
```
### canal构建
* 开启MySQL  binlog
* Linux修改my.cnf，Windows修改my.ini
```
server-id = 1
binlog+format = ROW
log_bin = mysql_bin
#log_bin = /User/hzllb/Documents/binlog/mysql-bin.log
```



