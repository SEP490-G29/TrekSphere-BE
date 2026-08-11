ALTER TABLE matching_group ADD COLUMN conversation_id UUID;
ALTER TABLE matching_group ADD CONSTRAINT fk_matching_group_conversation FOREIGN KEY (conversation_id) REFERENCES conversation(conversation_id);
