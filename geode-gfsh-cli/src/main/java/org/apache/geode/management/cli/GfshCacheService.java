package org.apache.geode.management.cli;

import static org.apache.geode.internal.serialization.DataSerializableFixedID.CLI_FUNCTION_RESULT;

import org.apache.geode.cache.Cache;
import org.apache.geode.internal.InternalDataSerializer;
import org.apache.geode.internal.cache.CacheService;
import org.apache.geode.management.internal.beans.CacheServiceMBeanBase;
import org.apache.geode.management.internal.cli.functions.CliFunctionResult;

public class GfshCacheService implements CacheService {

  @Override
  public boolean init(Cache cache) {
    InternalDataSerializer.getDSFIDSerializer().registerDSFID(CLI_FUNCTION_RESULT, CliFunctionResult.class);
    return true;
  }

  @Override
  public Class<? extends CacheService> getInterface() {
    return GfshCacheService.class;
  }

  @Override
  public CacheServiceMBeanBase getMBean() {
    return null;
  }
}
