package com.devicehive.model.rpc;

public enum Action {
    ERROR_RESPONSE,

    ECHO_REQUEST,
    ECHO_RESPONSE,

    NOTIFICATION_SEARCH_REQUEST,
    NOTIFICATION_SEARCH_RESPONSE,
    NOTIFICATION_INSERT_REQUEST,
    NOTIFICATION_SUBSCRIBE_REQUEST,
    NOTIFICATION_SUBSCRIBE_RESPONSE,
    NOTIFICATION_UNSUBSCRIBE_REQUEST,
    NOTIFICATION_UNSUBSCRIBE_RESPONSE,
    NOTIFICATION_EVENT,

    COMMAND_SEARCH_REQUEST,
    COMMAND_SEARCH_RESPONSE,
    COMMAND_INSERT_REQUEST,
    COMMAND_INSERT_RESPONSE,
    COMMAND_UPDATE_REQUEST,
    COMMAND_SUBSCRIBE_REQUEST,
    COMMAND_SUBSCRIBE_RESPONSE,
    COMMAND_UNSUBSCRIBE_REQUEST,
    COMMAND_UNSUBSCRIBE_RESPONSE,
    COMMAND_EVENT,
    COMMAND_UPDATE_EVENT,
    COMMAND_UPDATE_SUBSCRIBE_REQUEST,
    COMMAND_UPDATE_SUBSCRIBE_RESPONSE,
}
