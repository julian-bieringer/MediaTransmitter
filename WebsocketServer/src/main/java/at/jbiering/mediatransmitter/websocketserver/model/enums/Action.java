package at.jbiering.mediatransmitter.websocketserver.model.enums;

public enum Action
{
	ADD, REMOVE, TOGGLE, RETRIEVE_SUBSCRIBERS, SUBSCRIBER_LIST_UPDATE_REQUIRED,
	CREATE_FILE, CREATE_FILE_ACKNOWLEDGED, SEND_FILE_PART, RETRIEVE_FILE_PART,
    FILE_RECEIVED, END_FILE_REQUEST, END_FILE_ACKNOWLEDGED, END_FILE_ERROR,
    SEND_CHAT_MESSAGE, RETRIEVE_CHAT_MESSAGE
}