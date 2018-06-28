-- USERS
CREATE TABLE "internal_docs"(
    "id" VARCHAR NOT NULL,
    "body" CLOB NOT NULL,
    "updated_on" TIMESTAMP NOT NULL
);
ALTER TABLE "internal_docs" ADD CONSTRAINT "internal_docs_id" PRIMARY KEY("id");
