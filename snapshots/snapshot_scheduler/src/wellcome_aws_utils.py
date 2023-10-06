import collections
import datetime
import decimal
import json
import logging
import functools
import sys


def log_on_error(fn):
    @functools.wraps(fn)
    def wrapper(*args, **kwargs):
        try:
            return fn(*args, **kwargs)
        except Exception:
            print(f'args   = {args!r}', file=sys.stderr)
            print(f'kwargs = {kwargs!r}', file=sys.stderr)
            raise

    return wrapper


SNSEvent = collections.namedtuple('SNSEvent', 'subject message')


logger = logging.getLogger(__name__)


class EnhancedJSONEncoder(json.JSONEncoder):
    def default(self, obj):
        if isinstance(obj, datetime.datetime):
            return obj.isoformat()

        if isinstance(obj, decimal.Decimal):
            if float(obj).is_integer():
                return int(obj)
            else:
                return float(obj)

        return json.JSONEncoder.default(self, obj)


def publish_sns_message(sns_client,
                        topic_arn,
                        message,
                        subject="default-subject"):
    """
    Given a topic ARN and a series of key-value pairs, publish the key-value
    data to the SNS topic.
    """
    response = sns_client.publish(
        TopicArn=topic_arn,
        MessageStructure='json',
        Message=json.dumps({
            'default': json.dumps(
                message,
                cls=EnhancedJSONEncoder
            )
        }),
        Subject=subject
    )

    if response['ResponseMetadata']['HTTPStatusCode'] == 200:
        logger.debug('SNS: sent notification %s', response["MessageId"])
    else:
        raise RuntimeError(repr(response))

    return response
