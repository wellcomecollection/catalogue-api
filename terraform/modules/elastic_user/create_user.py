#!/usr/bin/env python
"""
This method checks if a user exists.
Exists: it updates the roles if needed with no password update, returns None
Missing: creates the user with a new password , returns username, password
"""
import elasticsearch
import secrets
import sys




def put_user_safely(es, username, password, roles):
    try:
        data = es.security.get_user(username=username)
        print(f"user: {username} exists")
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

        return None

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
        return username, password


if __name__ == '__main__':
    try:
        username = sys.argv[1]
        password = sys.argv[2]
        roles = sys.argv[3].split(",")
    except IndexError:
        sys.exit(f"Usage: {__file__} <USERNAME> <PASSWORD>")

    put_user_safely(username, password, roles)