import elasticsearch
import secrets

"""
This method checks if a user exists.
Exists: it updates the roles if needed with no password update, returns None
Missing: creates the user with a new password , returns username, password
"""
def put_user_safely(es, username, roles):
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
        service_password = secrets.token_hex()
        # es.security.put_user(
        #     username=username,
        #     body={
        #         "password": service_password,
        #         "roles": roles
        #     }
        # )
        print(f"{username} created")
        return username, service_password
