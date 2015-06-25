package com.devicehive.messages.bus.redis;

import com.devicehive.configuration.Constants;
import com.devicehive.json.adapters.TimestampAdapter;
import com.devicehive.model.DeviceNotification;
import com.devicehive.model.JsonStringWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.sql.Timestamp;
import java.util.*;

/**
 * Created by tmatvienko on 4/15/15.
 */
@Component
public class RedisNotificationService {
    private static final String KEY_FORMAT = "notification:%s:%s:%s";

    @Autowired
    private RedisConnector redis;

    @Value("${notification.expire.sec}")
    private int expireSec;

    public void save(DeviceNotification deviceNotification) {
        final String key = String.format(KEY_FORMAT, deviceNotification.getDeviceGuid(), deviceNotification.getId(), deviceNotification.getTimestamp());
        Map<String, String> notificationMap = new HashMap<>();
        notificationMap.put("id", deviceNotification.getId().toString());
        notificationMap.put("deviceGuid", deviceNotification.getDeviceGuid());
        notificationMap.put("notification", deviceNotification.getNotification());
        if (deviceNotification.getParameters() != null) {
            notificationMap.put("parameters", deviceNotification.getParameters().getJsonString());
        }
        notificationMap.put("timestamp", TimestampAdapter.formatTimestamp(deviceNotification.getTimestamp()));
        redis.setAll(key, notificationMap, expireSec);
    }

    public DeviceNotification get(final String key, final boolean filterByDate, final boolean filterByName, final Timestamp timestamp, final Collection<String> names) {
        Map<String, String> notificationMap = redis.getAll(key);
        if (!notificationMap.isEmpty()) {
            final Timestamp notificationTimestamp = TimestampAdapter.parseTimestamp(notificationMap.get("timestamp"));
            final boolean skip = (filterByDate && notificationTimestamp.compareTo(timestamp) <= 0 || (filterByName && !names.contains(notificationMap.get("notification"))));
            if (!skip) {
                DeviceNotification notification = new DeviceNotification();
                notification.setId(Long.valueOf(notificationMap.get("id")));
                notification.setDeviceGuid(notificationMap.get("deviceGuid"));
                notification.setNotification(notificationMap.get("notification"));
                if (notificationMap.get("parameters") != null) {
                    notification.setParameters(new JsonStringWrapper(notificationMap.get("parameters")));
                }
                notification.setTimestamp(notificationTimestamp);
                return notification;
            }
        }
        return null;
    }

    public DeviceNotification getByIdAndGuid(final Long id, final String guid) {
        final SortedSet<DeviceNotification> notifications = redis.fetch(String.format(KEY_FORMAT, guid, id, "*"), Constants.DEFAULT_TAKE, new DeviceNotificationComparator(),
                new Transformer<String, DeviceNotification>() {
                    @Override
                    public DeviceNotification apply(String key) {
                        return get(key, false, false, null, null);
                    }
                });
        return !(notifications == null || notifications.isEmpty()) ? notifications.first() : null;
    }

    public Collection<DeviceNotification> getByGuids(final Collection<String> guids, final Timestamp timestamp, final Collection<String> names, final Integer take) {
        final boolean filterByDate = timestamp != null;
        final boolean filterByName = !CollectionUtils.isEmpty(names);
        return getAllKeysByGuids(guids, take, new Transformer<String, DeviceNotification>() {
            @Override
            public DeviceNotification apply(String key) {
                return get(key, filterByDate, filterByName, timestamp, names);
            }
        });
    }

    public Collection<DeviceNotification> getAll(final Timestamp timestamp, final Collection<String> names, final Integer take) {
        final boolean filterByDate = timestamp != null;
        final boolean filterByName = !CollectionUtils.isEmpty(names);
        return redis.fetch(String.format(KEY_FORMAT, "*", "*", "*"), take, new DeviceNotificationComparator(),
                new Transformer<String, DeviceNotification>() {
                    @Override
                    public DeviceNotification apply(String key) {
                        return get(key, filterByDate, filterByName, timestamp, names);
                    }
                });
    }

    private Set<DeviceNotification> getAllKeysByGuids(final Collection<String> guids, Integer count, Transformer<String, DeviceNotification> transformer) {
        if (!CollectionUtils.isEmpty(guids)) {
            Comparator<DeviceNotification> comparator = new DeviceNotificationComparator();
            Set<DeviceNotification> accumulator = new TreeSet<>(comparator);
            for (String guid : guids) {
                accumulator.addAll(
                        redis.fetch(String.format(KEY_FORMAT, guid, "*", "*"), count, comparator, transformer)
                );
            }
            if (accumulator.size() > count) {
                List<DeviceNotification> sliced = new ArrayList<>(accumulator).subList(0, count);
                accumulator.clear();
                accumulator.addAll(sliced);
            }
            return accumulator;
        }
        return Collections.emptySet();
    }

    private class DeviceNotificationComparator implements Comparator<DeviceNotification> {

        @Override
        public int compare(DeviceNotification o1, DeviceNotification o2) {
            return o2.getTimestamp().compareTo(o1.getTimestamp());
        }
    }
}