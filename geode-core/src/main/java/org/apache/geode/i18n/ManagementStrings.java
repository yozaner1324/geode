package org.apache.geode.i18n;

import static org.apache.geode.distributed.ConfigurationProperties.CACHE_XML_FILE;
import static org.apache.geode.distributed.ConfigurationProperties.ENABLE_TIME_STATISTICS;
import static org.apache.geode.distributed.ConfigurationProperties.LOCATORS;
import static org.apache.geode.distributed.ConfigurationProperties.LOG_LEVEL;
import static org.apache.geode.distributed.ConfigurationProperties.MCAST_ADDRESS;
import static org.apache.geode.distributed.ConfigurationProperties.MCAST_PORT;
import static org.apache.geode.distributed.ConfigurationProperties.MEMCACHED_BIND_ADDRESS;
import static org.apache.geode.distributed.ConfigurationProperties.MEMCACHED_PORT;
import static org.apache.geode.distributed.ConfigurationProperties.MEMCACHED_PROTOCOL;
import static org.apache.geode.distributed.ConfigurationProperties.SERVER_BIND_ADDRESS;
import static org.apache.geode.distributed.ConfigurationProperties.SOCKET_BUFFER_SIZE;
import static org.apache.geode.distributed.ConfigurationProperties.STATISTIC_ARCHIVE_FILE;
import static org.apache.geode.distributed.ConfigurationProperties.USE_CLUSTER_CONFIGURATION;

import org.apache.geode.cache.server.CacheServer;
import org.apache.geode.distributed.ConfigurationProperties;

public abstract class ManagementStrings {

  public static final String LOG_LEVEL_VALUES =
      "Possible values for log-level include: ALL, TRACE, DEBUG, INFO, WARN, ERROR, FATAL, OFF.";

  /* 'start server' command */
  public static final String START_SERVER = "start server";
  public static final String START_SERVER__HELP = "Start a Geode Cache Server.";
  public static final String START_SERVER__ASSIGN_BUCKETS = "assign-buckets";
  public static final String START_SERVER__ASSIGN_BUCKETS__HELP =
      "Whether to assign buckets to the partitioned regions of the cache on server start.";
  public static final String START_SERVER__BIND_ADDRESS = "bind-address";
  public static final String START_SERVER__BIND_ADDRESS__HELP =
      "The IP address on which the Server will be bound.  By default, the Server is bound to all local addresses.";
  public static final String START_SERVER__CACHE_XML_FILE = CACHE_XML_FILE;
  public static final String START_SERVER__CACHE_XML_FILE__HELP =
      "Specifies the name of the cache XML file or resource to initialize the cache with when it is created. NOTE: If cluster configuration is enabled, then it will take precedence over this option";
  public static final String START_SERVER__CLASSPATH = "classpath";
  public static final String START_SERVER__CLASSPATH__HELP =
      "Location of user application classes required by the Server. The user classpath is prepended to the Server's classpath.";
  public static final String START_SERVER__DIR = "dir";
  public static final String START_SERVER__DIR__HELP =
      "Directory in which the Cache Server will be started and ran. The default is ./<server-member-name>";
  public static final String START_SERVER__DISABLE_DEFAULT_SERVER = "disable-default-server";
  public static final String START_SERVER__DISABLE_DEFAULT_SERVER__HELP =
      "Whether the Cache Server will be started by default.";
  public static final String START_SERVER__DISABLE_EXIT_WHEN_OUT_OF_MEMORY =
      "disable-exit-when-out-of-memory";
  public static final String START_SERVER__DISABLE_EXIT_WHEN_OUT_OF_MEMORY_HELP =
      "Prevents the JVM from exiting when an OutOfMemoryError occurs.";
  public static final String START_SERVER__ENABLE_TIME_STATISTICS = ENABLE_TIME_STATISTICS;
  public static final String START_SERVER__ENABLE_TIME_STATISTICS__HELP =
      "Causes additional time-based statistics to be gathered for Geode operations.";
  public static final String START_SERVER__FORCE = "force";
  public static final String START_SERVER__FORCE__HELP =
      "Whether to allow the PID file from a previous Cache Server run to be overwritten.";
  public static final String START_SERVER__GROUP__HELP =
      "Group(s) the Cache Server will be a part of.";
  public static final String START_SERVER__INCLUDE_SYSTEM_CLASSPATH = "include-system-classpath";
  public static final String START_SERVER__INCLUDE_SYSTEM_CLASSPATH__HELP =
      "Includes the System CLASSPATH on the Server's CLASSPATH. The System CLASSPATH is not included by default.";
  public static final String START_SERVER__INITIAL_HEAP = "initial-heap";
  public static final String START_SERVER__INITIAL_HEAP__HELP =
      "Initial size of the heap in the same format as the JVM -Xms parameter.";
  public static final String START_SERVER__JMX_MANAGER_HOSTNAME_FOR_CLIENTS__HELP =
      "Hostname provided to clients by the server for the location of a JMX Manager.";

  public static final String START_SERVER__J = "J";
  public static final String START_SERVER__J__HELP =
      "Argument passed to the JVM on which the server will run. For example, --J=-Dfoo.bar=true will set the system property \"foo.bar\" to \"true\".";
  public static final String START_SERVER__LOCATORS = LOCATORS;
  public static final String START_SERVER__LOCATORS__HELP =
      "Sets the list of Locators used by the Cache Server to join the appropriate Geode cluster.";
  public static final String START_SERVER__LOCK_MEMORY = ConfigurationProperties.LOCK_MEMORY;
  public static final String START_SERVER__LOCK_MEMORY__HELP =
      "Causes Geode to lock heap and off-heap memory pages into RAM. This prevents the operating system from swapping the pages out to disk, which can cause severe performance degradation. When you use this option, also configure the operating system limits for locked memory.";
  public static final String START_SERVER__LOCATOR_WAIT_TIME = "locator-wait-time";
  public static final String START_SERVER__LOCATOR_WAIT_TIME_HELP =
      "Sets the number of seconds the server will wait for a locator to become available during startup before giving up.";
  public static final String START_SERVER__LOG_LEVEL = LOG_LEVEL;
  public static final String START_SERVER__LOG_LEVEL__HELP =
      "Sets the level of output logged to the Cache Server log file.  " + LOG_LEVEL_VALUES;
  public static final String START_SERVER__MAXHEAP = "max-heap";
  public static final String START_SERVER__MAXHEAP__HELP =
      "Maximum size of the heap in the same format as the JVM -Xmx parameter.";
  public static final String START_SERVER__MCAST_ADDRESS = MCAST_ADDRESS;
  public static final String START_SERVER__MCAST_ADDRESS__HELP =
      "The IP address or hostname used to bind the UPD socket for multi-cast networking so the Cache Server can communicate with other members in the Geode cluster.  If mcast-port is zero, then mcast-address is ignored.";
  public static final String START_SERVER__MCAST_PORT = MCAST_PORT;
  public static final String START_SERVER__MCAST_PORT__HELP =
      "Sets the port used for multi-cast networking so the Cache Server can communicate with other members of the Geode cluster.  A zero value disables mcast.";
  public static final String START_SERVER__NAME = "name";
  public static final String START_SERVER__NAME__HELP =
      "The member name to give this Cache Server in the Geode cluster.";
  public static final String START_SERVER__MEMCACHED_PORT = MEMCACHED_PORT;
  public static final String START_SERVER__MEMCACHED_PORT__HELP =
      "Sets the port that the Geode memcached service listens on for memcached clients.";
  public static final String START_SERVER__MEMCACHED_PROTOCOL = MEMCACHED_PROTOCOL;
  public static final String START_SERVER__MEMCACHED_PROTOCOL__HELP =
      "Sets the protocol that the Geode memcached service uses (ASCII or BINARY).";
  public static final String START_SERVER__MEMCACHED_BIND_ADDRESS = MEMCACHED_BIND_ADDRESS;
  public static final String START_SERVER__MEMCACHED_BIND_ADDRESS__HELP =
      "Sets the IP address the Geode memcached service listens on for memcached clients. The default is to bind to the first non-loopback address for this machine.";
  public static final String START_SERVER__OFF_HEAP_MEMORY_SIZE =
      ConfigurationProperties.OFF_HEAP_MEMORY_SIZE;
  public static final String START_SERVER__OFF_HEAP_MEMORY_SIZE__HELP =
      "The total size of off-heap memory specified as off-heap-memory-size=<n>[g|m]. <n> is the size. [g|m] indicates whether the size should be interpreted as gigabytes or megabytes. A non-zero size causes that much memory to be allocated from the operating system and reserved for off-heap use.";
  public static final String START_SERVER__PROPERTIES = "properties-file";
  public static final String START_SERVER__PROPERTIES__HELP =
      "The gemfire.properties file for configuring the Cache Server's distributed system. The file's path can be absolute or relative to the gfsh working directory.";
  public static final String START_SERVER__REDIS_PORT = ConfigurationProperties.REDIS_PORT;
  public static final String START_SERVER__REDIS_PORT__HELP =
      "Sets the port that the Geode Redis service listens on for Redis clients.";
  public static final String START_SERVER__REDIS_BIND_ADDRESS =
      ConfigurationProperties.REDIS_BIND_ADDRESS;
  public static final String START_SERVER__REDIS_BIND_ADDRESS__HELP =
      "Sets the IP address the Geode Redis service listens on for Redis clients. The default is to bind to the first non-loopback address for this machine.";
  public static final String START_SERVER__REDIS_PASSWORD = ConfigurationProperties.REDIS_PASSWORD;
  public static final String START_SERVER__REDIS_PASSWORD__HELP =
      "Sets the authentication password for GeodeRedisServer";
  public static final String START_SERVER__SECURITY_PROPERTIES = "security-properties-file";
  public static final String START_SERVER__SECURITY_PROPERTIES__HELP =
      "The gfsecurity.properties file for configuring the Server's security configuration in the distributed system. The file's path can be absolute or relative to gfsh directory.";
  public static final String START_SERVER__REBALANCE = "rebalance";
  public static final String START_SERVER__REBALANCE__HELP =
      "Whether to initiate rebalancing across the Geode cluster.";
  public static final String START_SERVER__SERVER_BIND_ADDRESS = SERVER_BIND_ADDRESS;
  public static final String START_SERVER__SERVER_BIND_ADDRESS__HELP =
      "The IP address that this distributed system's server sockets in a client-server topology will be bound. If set to an empty string then all of the local machine's addresses will be listened on.";
  public static final String START_SERVER__SERVER_PORT = "server-port";
  public static final String START_SERVER__SERVER_PORT__HELP =
      "The port that the distributed system's server sockets in a client-server topology will listen on.  The default server-port is "
          + CacheServer.DEFAULT_PORT + ".";
  public static final String START_SERVER__SPRING_XML_LOCATION = "spring-xml-location";
  public static final String START_SERVER__SPRING_XML_LOCATION_HELP =
      "Specifies the location of a Spring XML configuration file(s) for bootstrapping and configuring a Geode Server.";
  public static final String START_SERVER__STATISTIC_ARCHIVE_FILE = STATISTIC_ARCHIVE_FILE;
  public static final String START_SERVER__STATISTIC_ARCHIVE_FILE__HELP =
      "The file that statistic samples are written to.  An empty string (default) disables statistic archival.";
  public static final String START_SERVER__USE_CLUSTER_CONFIGURATION = USE_CLUSTER_CONFIGURATION;
  public static final String START_SERVER__USE_CLUSTER_CONFIGURATION__HELP =
      "When set to true, the server requests the configuration from locator's cluster configuration service.";
  public static final String START_SERVER__GENERAL_ERROR_MESSAGE =
      "An error occurred while attempting to start a Geode Cache Server: %1$s";
  public static final String START_SERVER__PROCESS_TERMINATED_ABNORMALLY_ERROR_MESSAGE =
      "The Cache Server process terminated unexpectedly with exit status %1$d. Please refer to the log file in %2$s for full details.%n%n%3$s";
  public static final String START_SERVER__RUN_MESSAGE = "Starting a Geode Server in %1$s...";


  public static final String START_SERVER__CRITICAL__HEAP__PERCENTAGE = "critical-heap-percentage";
  public static final String START_SERVER__CRITICAL__HEAP__HELP =
      "Set the percentage of heap at or above which the cache is considered in danger of becoming inoperable due to garbage collection pauses or out of memory exceptions";

  public static final String START_SERVER__EVICTION__HEAP__PERCENTAGE = "eviction-heap-percentage";
  public static final String START_SERVER__EVICTION__HEAP__PERCENTAGE__HELP =
      "Set the percentage of heap at or above which the eviction should begin on Regions configured for HeapLRU eviction. Changing this value may cause eviction to begin immediately. "
          + "Only one change to this attribute or critical heap percentage will be allowed at any given time and its effect will be fully realized before the next change is allowed. This feature requires additional VM flags to perform properly. ";

  public static final String START_SERVER__CRITICAL_OFF_HEAP_PERCENTAGE =
      "critical-off-heap-percentage";
  public static final String START_SERVER__CRITICAL_OFF_HEAP__HELP =
      "Set the percentage of off-heap memory at or above which the cache is considered in danger of becoming inoperable due to out of memory exceptions";

  public static final String START_SERVER__EVICTION_OFF_HEAP_PERCENTAGE =
      "eviction-off-heap-percentage";
  public static final String START_SERVER__EVICTION_OFF_HEAP_PERCENTAGE__HELP =
      "Set the percentage of off-heap memory at or above which the eviction should begin on Regions configured for off-heap and HeapLRU eviction. Changing this value may cause eviction to begin immediately."
          + " Only one change to this attribute or critical off-heap percentage will be allowed at any given time and its effect will be fully realized before the next change is allowed.";
  public static final String START_SERVER__HOSTNAME__FOR__CLIENTS = "hostname-for-clients";
  public static final String START_SERVER__HOSTNAME__FOR__CLIENTS__HELP =
      "Sets the ip address or host name that this cache server is to listen on for client connections."
          + "Setting a specific hostname-for-clients will cause server locators to use this value when telling clients how to connect to this cache server. This is useful in the case where the cache server may refer to itself with one hostname, but the clients need to use a different hostname to find the cache server."
          + "The value \"\" causes the bind-address to be given to clients."
          + "A null value will be treated the same as the default \"\".";


  public static final String START_SERVER__LOAD__POLL__INTERVAL = "load-poll-interval";
  public static final String START_SERVER__LOAD__POLL__INTERVAL__HELP =
      "Set the frequency in milliseconds to poll the load probe on this cache server";


  public static final String START_SERVER__MAX__CONNECTIONS = "max-connections";
  public static final String START_SERVER__MAX__CONNECTIONS__HELP =
      "Sets the maximum number of client connections allowed. When the maximum is reached the cache server will stop accepting connections";

  public static final String START_SERVER__MAX__THREADS = "max-threads";
  public static final String START_SERVER__MAX__THREADS__HELP =
      "Sets the maximum number of threads allowed in this cache server to service client requests. The default of 0 causes the cache server to dedicate a thread for every client connection";

  public static final String START_SERVER__MAX__MESSAGE__COUNT = "max-message-count";
  public static final String START_SERVER__MAX__MESSAGE__COUNT__HELP =
      "Sets maximum number of messages that can be enqueued in a client-queue.";

  public static final String START_SERVER__MESSAGE__TIME__TO__LIVE = "message-time-to-live";
  public static final String START_SERVER__MESSAGE__TIME__TO__LIVE__HELP =
      "Sets the time (in seconds ) after which a message in the client queue will expire";

  public static final String START_SERVER__SOCKET__BUFFER__SIZE = SOCKET_BUFFER_SIZE;
  public static final String START_SERVER__SOCKET__BUFFER__SIZE__HELP =
      "Sets the buffer size in bytes of the socket connection for this CacheServer. The default is 32768 bytes.";

  public static final String START_SERVER__TCP__NO__DELAY = "tcp-no-delay";
  public static final String START_SERVER__TCP__NO__DELAY__HELP =
      "Configures the tcpNoDelay setting of sockets used to send messages to clients. TcpNoDelay is enabled by default";

  // Exception messages
  public static final String PROVIDE_EITHER_MEMBER_OR_GROUP_MESSAGE =
      "Please provide either \"member\" or \"group\" option.";

  // General CLI Error messages
  public static final String ERROR__MSG__COULD_NOT_INSTANTIATE_CLASS_0_SPECIFIED_FOR_1 =
      "Could not instantiate class \"{0}\" specified for \"{1}\".";
  public static final String ERROR__MSG__COULD_NOT_ACCESS_CLASS_0_SPECIFIED_FOR_1 =
      "Could not access class \"{0}\" specified for \"{1}\".";
  public static final String ERROR__MSG__COULD_NOT_FIND_CLASS_0_SPECIFIED_FOR_1 =
      "Could not find class \"{0}\" specified for \"{1}\".";
  public static final String ERROR__MSG__CLASS_0_SPECIFIED_FOR_1_IS_NOT_OF_EXPECTED_TYPE =
      "Class \"{0}\" specified for \"{1}\" is not of an expected type.";

  // Create Region
  public static final String CREATE_REGION__KEYCONSTRAINT = "key-constraint";
  public static final String CREATE_REGION__KEYCONSTRAINT__HELP =
      "Fully qualified class name of the objects allowed as region keys. Ensures that keys for region entries are all of the same class.";
  public static final String CREATE_REGION__VALUECONSTRAINT = "value-constraint";
  public static final String CREATE_REGION__VALUECONSTRAINT__HELP =
      "Fully qualified class name of the objects allowed as region values. If not specified then region values can be of any class.";
  public static final String CREATE_REGION__MSG__OBJECT_SIZER_MUST_BE_OBJECTSIZER_AND_DECLARABLE =
      "eviction-object-sizer must implement both ObjectSizer and Declarable interfaces";

  // Support stack-traces
  public static final String STACK_TRACE_FOR_MEMBER = "*** Stack-trace for member ";
}
