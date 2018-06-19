-- USERS
CREATE TABLE "plugin_metadata"(
    "id" VARCHAR NOT NULL,
    "metadata" CLOB NOT NULL,
    "updated_on" TIMESTAMP NOT NULL
);
ALTER TABLE "plugin_metadata" ADD CONSTRAINT "plugin_metadata_id" PRIMARY KEY("id");
