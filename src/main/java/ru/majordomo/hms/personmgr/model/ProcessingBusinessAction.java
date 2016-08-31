package ru.majordomo.hms.personmgr.model;

import org.springframework.data.redis.core.RedisHash;

/**
 * ProcessingBusinessAction
 */
@RedisHash("processingBusinessAction")
public class ProcessingBusinessAction extends BusinessAction {
}
