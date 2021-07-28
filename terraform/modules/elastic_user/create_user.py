#!/usr/bin/env python
"""
This method checks if a user exists.
Exists: it updates the roles if needed with no password update, returns None
Missing: creates the user with a new password , returns username, password
"""
import elasticsearch
import sys
from elastic import get_es_client


def create_user(es, username, password, roles):
    #  if a user exists, throw an error
    try:
        data = es.security.get_user(username=username)
        if data:
            raise Exception(f"user: {username} exists, exiting")
    except elasticsearch.exceptions.NotFoundError:
        print(f"user: {username} missing, creating...")

    es.security.put_user(
        username=username,
        body={
            "password": password,
            "roles": roles
        }
    )
    print(f"{username} created")


if __name__ == '__main__':
    try:
        username = sys.argv[1]
        password = sys.argv[2]
        roles = sys.argv[2].split(",")
    except IndexError:
        sys.exit(f"Usage: {__file__} <USERNAME> <PASSWORD> <ROLES>")

    es = get_es_client()
    create_user(es=es, username=username, password=password, roles=roles)
