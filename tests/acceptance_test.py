import os
import subprocess
import requests
import json
from signal import SIGINT
from time import sleep
from pump_client.PumpClient import PumpClient
from nose.tools import assert_equal, assert_in, assert_is_not_none

def end_to_end_test():
    """Launch and query cluster"""

    stream = os.environ['KINESIS_STREAM']

    conf_file = os.environ['WATERSHED_CONFIG']
    assert os.path.isfile(conf_file)

    with open(conf_file, "r") as f:
        conf = json.load(f)

    private_key_file = os.environ['PRIVATE_KEY_FILE']
    assert os.path.isfile(private_key_file)

    assert_equal(0, subprocess.check_call(
        [
            "python3",
            "-m",
            "watershed",
            "u",
            "-c",
            conf_file,
            "-f"
        ]
    ))

    print("Launching cluster.")
    launch_output = subprocess.check_output(
        [
            "python3",
            "-m",
            "watershed",
            "l",
            "-c",
            conf_file,
            "-w"
        ]
    ).decode()

    beg_index = launch_output.rfind('j-')
    end_index = launch_output.rfind('\'')

    cluster_id = launch_output[beg_index:end_index]

    forwarding_process = None
    try:
        print("Forwarding ports.")
        forwarding_process = subprocess.Popen(
            [
                "python3",
                "-m",
                "watershed",
                "f",
                "-i",
                cluster_id,
                "-k",
                private_key_file
            ],
            preexec_fn=os.setsid
        )
        sleep(1)

        r = None
        for retry in range(0, 10):
            try:
                r = requests.post(
                    'http://localhost:8047/query.json',
                    json.dumps({
                        "queryType": "SQL",
                        "query": "SELECT COUNT(employee_id) cnt FROM cp.`employee.json`"
                    }),
                    headers={
                        "Content-Type": "application/json"
                    }
                )
            except Exception:
                pass

            if r is not None and hasattr(r, "status_code"):
                assert_equal(r.status_code, 200)
                query = json.loads(r.text)
                assert_equal(len(query["rows"]), 1)
                assert_in("cnt", query["rows"][0])
                assert_equal(query["rows"][0]["cnt"], "1155")
                break
            if retry == 10 and r is None:
                raise ConnectionRefusedError("Unable to connect to Drill.")
            sleep(1)
        r = None
        for retry in range(0, 10):
            try:
                r = requests.post(
                    'http://localhost:8047/query.json',
                    json.dumps({
                        "queryType": "SQL",
                        "query": "SELECT * FROM `{}`.`sample_data`.* LIMIT 1".format(conf["AWS"]["S3"]["resourcesBucket"])
                    }),
                    headers={
                        "Content-Type": "application/json"
                    }
                )
            except Exception:
                pass

            if r is not None and hasattr(r, "status_code"):
                assert_equal(r.status_code, 200)
                query = json.loads(r.text)
                assert_in("eventId", query["columns"])
                assert_in("seller", query["columns"])
                assert_in("buyer", query["columns"])
                assert_in("price", query["columns"])
                assert_in("productId", query["columns"])
                assert_in("kinesisStreamName", query["columns"])
                assert_in("timeCreated", query["columns"])
                assert_in("valid", query["columns"])
                assert_in("partitionKey", query["columns"])
                assert_in("rawData", query["columns"])
                assert_equal("true", query["rows"][0]["valid"])
                assert_equal(query["rows"][0]["eventId"], query["rows"][0]["partitionKey"])
                break
            if retry == 10 and r is None:
                raise ConnectionRefusedError("Unable to connect to Drill.")
            sleep(1)
        r = None
        for retry in range(0, 10):
            try:
                r = requests.post(
                    'http://localhost:8047/query.json',
                    json.dumps({
                        "queryType": "SQL",
                        "query": "SELECT COUNT(*) cnt FROM `{}`.`sample_data`.* WHERE price>80".format(conf["AWS"]["S3"]["resourcesBucket"])
                    }),
                    headers={
                        "Content-Type": "application/json"
                    }
                )
            except Exception:
                pass

            if r is not None and hasattr(r, "status_code"):
                assert_equal(r.status_code, 200)
                query = json.loads(r.text)
                assert_equal(len(query["rows"]), 1)
                assert_in("cnt", query["rows"][0])
                assert_equal(query["rows"][0]["cnt"], "257")
                break
            if retry == 10 and r is None:
                raise ConnectionRefusedError("Unable to connect to Drill.")
            sleep(1)

        client = PumpClient("http://localhost:8080/pump")

        job = client.preview_job("SELECT * FROM `{}`.`sample_data`.* WHERE price>80".format(conf["AWS"]["S3"]["resourcesBucket"]), 1)
        assert_equal(job["count"], 257)
        assert_in("partitionKey", job["rows"][0])
        assert_in("rawData", job["rows"][0])

        job = client.create_job("SELECT * FROM `{}`.`sample_data`.* WHERE price>80".format(conf["AWS"]["S3"]["resourcesBucket"]), stream, False, True, True)
        assert_is_not_none(job["jobId"])
        job_id = job["jobId"]
        assert_equal(job["stage"], "NOT_STARTED")

        job = client.get_job(job_id, True, False)
        assert_equal(job["stage"], "COMPLETED_SUCCESS")
        assert_equal(job["successfulRecordCount"], 257)
        assert_equal(len(job["processingErrors"]), 0)
    finally:
        if forwarding_process is not None:
            forwarding_process.send_signal(SIGINT)

        subprocess.call(
            [
                "python3",
                "-m",
                "watershed",
                "t",
                "-i",
                cluster_id,
            ]
        )
