#!/usr/bin/env python
"""
This method checks if a user exists.
Exists: it updates the roles if needed with no password update, returns None
Missing: creates the user with a new password , returns username, password
"""
import elasticsearch
import sys
from elastic import get_es_client


def update_user(es, username, roles):
    try:
        data = es.security.get_user(username=username)
    except elasticsearch.exceptions.NotFoundError:
        print(f"couldn't find user {username}, exiting")
        raise

    if data[username]["roles"] != roles:
        print(f"user: {username} updating roles...")
        es.security.put_user(
            username=username,
            body={
                "roles": roles
            }
        )
        print(f"user: {username} roles updated")
    else:
        print(f"user: {username} has no role updates")


if __name__ == '__main__':
    try:
        username = sys.argv[1]
        roles = sys.argv[2].split(",")
    except IndexError:
        sys.exit(f"Usage: {__file__} <USERNAME> <ROLES>")

    es = get_es_client()
    update_user(es=es, username=username, roles=roles)
