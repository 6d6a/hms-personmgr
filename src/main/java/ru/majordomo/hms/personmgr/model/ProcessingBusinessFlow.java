package ru.majordomo.hms.personmgr.model;

import org.springframework.data.redis.core.RedisHash;

/**
 * ProcessingBusinessFlow
 */
@RedisHash("processingBusinessFlow")
public class ProcessingBusinessFlow extends BusinessFlow {
}
