# Copyright (C) 2015 Commerce Technologies, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

from time import sleep

import boto3

from watershed.aws_tools.s3 import upload_stream_archive_configuration


def _wait_till_not_running_steps(cluster_id, profile='default'):
    emr_client = boto3.session.Session(profile_name=profile).client('emr')

    try:
        for attempts in range(0, 60):
            state = emr_client.describe_cluster(ClusterId=cluster_id)['Cluster']['Status']['State']
            if state == 'WAITING':
                return "Cluster is ready."
            elif state == 'TERMINATING' or state == 'TERMINATED_WITH_ERRORS' or state == 'TERMINATED':
                return "Cluster was terminated while waiting with the state: " + state
            sleep(10)
    except Exception as aws_except:
        raise ValueError(aws_except)

    return "Cluster failed to finish step timely."


def get_master_address(cluster_id, profile='default'):
    emr_client = boto3.session.Session(profile_name=profile).client('emr')

    if cluster_id is None:
        raise ValueError("Cluster ID cannot be none.")
    try:
        json_description = emr_client.describe_cluster(ClusterId=cluster_id)
        print(json_description)
        return json_description['Cluster']['MasterPublicDnsName']
    except Exception as aws_except:
        raise ValueError(aws_except)


def wait_for_cluster(cluster_id, profile="default"):
    emr_client = boto3.session.Session(profile_name=profile).client('emr')
    cluster_start_waiter = emr_client.get_waiter('cluster_running')
    cluster_start_waiter.wait(ClusterId=cluster_id)
    _wait_till_not_running_steps(cluster_id, profile)


def launch_emr_cluster(s3_config=None, emr_config=None, profile="default", wait_until_ready=False, logging=False):
    emr_client = boto3.session.Session(profile_name=profile).client('emr')

    s3_url = "s3://"+s3_config['resourcesBucket']+"/"+s3_config['resourcesPrefix']

    bootstrap_actions = [
        {
            'Name': 'Install Impala',
            'ScriptBootstrapAction': {
                'Path': 's3://us-east-1.elasticmapreduce/libs/impala/setup-impala',
                'Args': [
                    "--base-path",
                    "s3://us-east-1.elasticmapreduce",
                    "--impala-version",
                    "latest"
                ]
            }
        },
        {
            'Name': 'configure-hadoop',
            'ScriptBootstrapAction': {
                'Path': 's3://elasticmapreduce/bootstrap-actions/configure-hadoop',
                'Args': [
                    "--hdfs-key-value",
                    "dfs.replication=3",
                    "--core-key-value",
                    "io.compression.codecs=org.apache.hadoop.io.compress.GzipCodec,org.apache.hadoop.io.compress.DefaultCodec,org.apache.hadoop.io.compress.BZip2Codec,org.apache.hadoop.io.compress.SnappyCodec"
                ]
            }
        },
        {
            'Name': 'setup_drill',
            'ScriptBootstrapAction': {
                'Path': s3_url+'/emr/exec/setup_drill',
                'Args': []
            }
        },
        {
            'Name': 'setup_drill_lib_aws',
            'ScriptBootstrapAction': {
                'Path': s3_url+'/emr/exec/setup_drill_lib_aws',
                'Args': []
            }
        },
        {
            'Name': 'configure_drill_storage_dfs',
            'ScriptBootstrapAction': {
                'Path': s3_url+'/emr/exec/configure_drill_storage_dfs',
                'Args': []
            }
        },
        {
            'Name': 'configure_drill_storage_streams',
            'ScriptBootstrapAction': {
                'Path': s3_url+'/emr/exec/configure_drill_storage_streams',
                'Args': []
            }
        },
        {
            'Name': 'setup_python',
            'ScriptBootstrapAction': {
                'Path': s3_url+'/emr/exec/setup_python',
                'Args': []
            }
        }
    ]
    steps = [
        {
            'Name': 'Install Hive',
            'HadoopJarStep': {
                'Jar': "s3://us-east-1.elasticmapreduce/libs/script-runner/script-runner.jar",
                'Args': [
                    "s3://us-east-1.elasticmapreduce/libs/hive/hive-script",
                    "--install-hive",
                    "--base-path",
                    "s3://us-east-1.elasticmapreduce/libs/hive",
                    "--hive-versions",
                    "latest"
                ]
            },
            'ActionOnFailure': 'TERMINATE_CLUSTER'
        }
    ]

    instance_groups = []

    for group in emr_config['instanceGroups']:
        if "Name" not in group:
            group["Name"] = "EMR " + group["InstanceRole"]
        instance_groups.append(group)

    instances = {
        'KeepJobFlowAliveWhenNoSteps': True,
        'InstanceGroups': instance_groups,
        'Ec2KeyName': emr_config['ec2KeyName'] if emr_config['ec2KeyName'] is not None else "",
        'Ec2SubnetId': emr_config['ec2SubnetId'] if emr_config['ec2SubnetId'] is not None else "",
        'AdditionalMasterSecurityGroups': emr_config['additionalMasterSecurityGroups'] if emr_config['additionalMasterSecurityGroups'] is not None else []
    }
    try:
        if logging:
            return_json = emr_client.run_job_flow(
                Name=emr_config['clusterName'],
                AmiVersion=emr_config['amiVersion'],
                Instances=instances,
                VisibleToAllUsers=True,
                LogUri=s3_url+"/emr/logs",
                Steps=steps,
                BootstrapActions=bootstrap_actions,
                ServiceRole=emr_config['roles']['service'],
                JobFlowRole=emr_config['roles']['ec2'],
                Tags=emr_config['tags']
            )
        else:
            return_json = emr_client.run_job_flow(
                Name=emr_config['clusterName'],
                AmiVersion=emr_config['amiVersion'],
                Instances=instances,
                VisibleToAllUsers=True,
                Steps=steps,
                BootstrapActions=bootstrap_actions,
                ServiceRole=emr_config['roles']['service'],
                JobFlowRole=emr_config['roles']['ec2'],
                Tags=emr_config['tags']
            )
        print(return_json)
        if wait_until_ready:
            print("Waiting option selected. Cluster can take more than 5 minutes to start...")
            cluster_id = return_json['JobFlowId']
            wait_for_cluster(cluster_id, profile)
            _wait_till_not_running_steps(cluster_id, profile)
            print("Cluster '{0}' ready.".format(cluster_id))
        return return_json['JobFlowId']
    except Exception as aws_except:
        raise ValueError(aws_except)


def terminate_emr_cluster(cluster_ids=None, profile='default'):
    emr_client = boto3.session.Session(profile_name=profile).client('emr')

    try:
        emr_client.terminate_job_flows(JobFlowIds=cluster_ids)
        print("Attempted to terminate the following cluster Ids:")
        for cluster_id in cluster_ids:
            print("  * '"+cluster_id+"'")
    except Exception as aws_except:
        raise ValueError(aws_except)


def configure_stream_tables(cluster_id, s3_config=None, stream_configs=None, profile='default'):
    emr_client = boto3.session.Session(profile_name=profile).client('emr')

    s3_url = "s3://" + s3_config['resourcesBucket'] + "/" + s3_config['resourcesPrefix']

    steps_to_add = []
    for stream_config in stream_configs:
        steps_to_add.append({
            'Name': 'hive_create_table_' + stream_config['table'] + '_for_stream_' + stream_config['stream'],
            'HadoopJarStep': {
                'Jar': "s3://us-east-1.elasticmapreduce/libs/script-runner/script-runner.jar",
                'Args': [
                    "s3://us-east-1.elasticmapreduce/libs/hive/hive-script",
                    "--run-hive-script",
                    "--hive-versions",
                    "latest",
                    "--args",
                    '-f',
                    s3_url + '/emr/hive/create_table_for_stream',
                    '-hiveconf',
                    'tablename=' + stream_config['table'],
                    '-hiveconf',
                    'streamname=' + stream_config['stream']
                ]
            },
            'ActionOnFailure': 'CANCEL_AND_WAIT'
        })

    try:
        return_json = emr_client.add_job_flow_steps(
            JobFlowId=cluster_id,
            Steps=steps_to_add
        )
        print(return_json)
    except Exception as aws_except:
        raise ValueError(aws_except)


def configure_stream_archives(cluster_id, s3_config=None, archive_configs=None, profile='default'):
    archives = {}

    for archive in archive_configs:
        dfs_url = archive['dfsUrl']
        archive_path = archive['path']
        archive_name = archive['name']
        if dfs_url not in archives:
            archives[dfs_url] = []
        if archive_path not in archives[dfs_url]:
            # TODO verify names are unique
            archives[dfs_url].append({'archive_path': archive_path, 'archive_name': archive_name})

    conf_files = []
    for dfs_url in archives.keys():
        conf_file = {
            "name": dfs_url.split('@')[1],
            "config": {
                "type": "file",
                "enabled": "true",
                "connection": dfs_url,
                "workspaces": {},
                "formats": None
            }
        }

        for archive_defn in archives[dfs_url]:
            conf_file["config"]["workspaces"][archive_defn['archive_name']] = {
                "location": archive_defn['archive_path'],
                "writable": False
            }
        conf_files.append(conf_file)

    config_paths = upload_stream_archive_configuration(s3_config, conf_files, profile)

    emr_client = boto3.session.Session(profile_name=profile).client('emr')

    s3_url = "s3://" + s3_config['resourcesBucket'] + "/" + s3_config['resourcesPrefix']

    args_list = [
        s3_url + '/emr/python/upload_drill_storage_configuration.py',
        s3_config["resourcesBucket"],
        s3_config["accessKey"],
        s3_config["secretKey"]
    ]

    args_list += config_paths

    new_step = {
        'Name': 'configure_drill_storage_stream_archives',
        'HadoopJarStep': {
            'Jar': "s3://us-east-1.elasticmapreduce/libs/script-runner/script-runner.jar",
            'Args': args_list
        },
        'ActionOnFailure': 'CANCEL_AND_WAIT'
    }

    try:
        return_json = emr_client.add_job_flow_steps(
            JobFlowId=cluster_id,
            Steps=[
                new_step
            ]
        )
        print(return_json)
    except Exception as aws_except:
        raise ValueError(aws_except)


def configure_streams(cluster_id, s3_config=None, stream_configs=None, archive_configs=None, profile='default', wait_until_ready=False):
    try:
        configure_stream_tables(cluster_id, s3_config, stream_configs, profile)
        configure_stream_archives(cluster_id, s3_config, archive_configs, profile)
        if wait_until_ready:
            print("Waiting option selected. Waiting until step(s) complete.")
            print(_wait_till_not_running_steps(cluster_id, profile))
    except Exception as aws_except:
        raise ValueError(aws_except)
