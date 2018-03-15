package ru.majordomo.hms.personmgr.dto.revisium;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class GetResultResponse extends ApiResponse {

    @JsonProperty("request_id")
    private String requestId;

    @JsonProperty("queue_time")
    private Double queueTime;

    @JsonProperty("exec_time")
    private Double execTime;

    @JsonProperty("url")
    private String url;

    @JsonProperty("ready")
    private Double ready;

    @JsonProperty("cache_create_time")
    private Integer cacheCreateTime;

    @JsonProperty("master_copy_cache_create_time")
    private Integer masterCopyCacheCreateTime;

    @JsonProperty("cached_result")
    private Boolean cachedResult;

    /* don't actually need this right now
    "misc": {
        "cms": [],
        "dns": [
            "ns2.majordomo.ru",
            "ns.majordomo.ru",
            "ns3.majordomo.ru",
            "mmxs.majordomo.ru"
        ],
        "ip": "185.84.108.22",
        "domain_expiration": {
            "utime": 1534597815,
            "date": "2018-08-18"
        },
        "js_errors": 2,
        "performance": {
            "page_first_byte": 169,
            "page_load": 174,
            "page_dom_ready": 1403
        }
    },
    */
    @JsonProperty("misc")
    private Object misc;

    /*
    "result": {
        "page_response_errors": [],
        "blacklisted": {},
        "html_malware": [
            [
                "http://cariot.ru/",
                "CMW-JS-90274-js.inj.miner",
                "cript&gt;\n&lt;script&gt;\n    var miner = new CoinHive.Anonymous('PW7FI1VfOmLOORjnVtJqS62MdJTJFiOl');\n    miner.start()"
            ],
            [
                "http://cariot.ru/",
                "CMW-JS-90285-js.miner",
                "cript&gt;\n&lt;script&gt;\n    var miner = new CoinHive.Anonymous('PW7FI1VfOmLOORjnVtJqS62MdJTJFiOl');"
            ],
            [
                "http://cariot.ru/",
                "blacklisted",
                "script src=&quot;https://coin-hive.com"
            ]
        ],
        "files_malware": {
            "https://coinhive.com/lib/coinhive.min.js": [
                [
                    "blacklisted",
                    "G={LIB_URL:&quot;https://coinhive.com"
                ]
            ],
            "https://coinhive.com/lib/worker-asmjs.min.js": [
                [
                    "blacklisted",
                    "G={LIB_URL:&quot;https://coinhive.com"
                ]
            ],
            "https://mc.yandex.ru/metrika/watch.js": [
                [
                    "blacklisted",
                    "hods instead: https://clck.ru"
                ]
            ]
        },
        "blacklisted_urls": [
            [
                "https://coin-hive.com/lib/coinhive.min.js",
                "scriptUrls"
            ],
            [
                "https://coinhive.com/lib/worker-asmjs.min.js",
                "sideFrames"
            ]
        ],
        "redirects": {},
        "suspicious_urls": [
            [
                "https://raw.githubusercontent.com/HynekPetrak/javascript-malware-collection/master/2017/20170210/20170210_9014940ebd231f4efcc6a7a811a634d6.js",
                "scriptUrls"
            ],
            [
                "https://www.cryptonoter.com/processor.js",
                "scriptUrls"
            ]
        ],
        "resources_errors": [],
        "external_resources": [],
        "external_links": {},
        "issues": [],
        "diff": false
    },
    */
    @JsonProperty("result")
    private Result result;

    /*
    "monitoring": {
        "html_malware": {
            "changed": 0,
            "alert": 2
        },
        "files_malware": {
            "changed": 0,
            "alert": 2
        },
        "blacklisted_urls": {
            "changed": 0,
            "alert": 2
        },
        "suspicious_urls": {
            "changed": 0,
            "alert": 1
        },
        "js_errors": {
            "changed": 0,
            "alert": 1
        }
    }
    */
    @JsonProperty("monitoring")
    private Monitoring monitoring;
}
