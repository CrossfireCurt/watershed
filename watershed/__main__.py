#!/usr/bin/env python3

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

import argparse
import json
import os

from watershed.aws_tools.s3 import upload_resources
from watershed.aws_tools.s3 import upload_pump
from watershed.aws_tools.emr import launch_emr_cluster, terminate_emr_cluster, configure_streams, wait_for_cluster
from watershed.ssh_tools.ssh import forward_necessary_ports


def get_argument_parser():
    _fc_description = """
Python/Boto solution which compliments Amazon Kinesis with:
  - Long-term archival of records
  - Making current and archived records accessible to SQL-based exploration and analysis
  - Replay of archived records:
    + supports key-value compaction
    + supports bounded replay
    + supports filtered replay
    + supports record annotation"""

    _config_file_args = [
        "-c",
        "--config-file"
    ]
    _config_file_kwargs = dict(
        action="store",
        help="Specify the configuration json file",
        required=True,
        type=argparse.FileType('r')
    )
    _cluster_id_args = [
        "-i",
        "--cluster-id"
    ]
    _cluster_id_kwargs = dict(
        action="store",
        help="Specify the cluster id for the operation to be performed on",
        required=True,
        type=str
    )
    _private_key_args = [
        "-k",
        "--private-key"
    ]
    _private_key_kwargs = dict(
        action="store",
        help="Specify the location of the private key file",
        required=True
    )
    _profile_args = [
        "-p",
        "--profile"
    ]
    _profile_kwargs = dict(
        action="store",
        help="Specify the AWS profile to use. Defaults to 'default'",
        default='default'
    )
    _wait_until_ready_args = [
        '-w',
        '--wait-until-ready'
    ]
    _wait_until_ready_kwargs = dict(
        action="store_const",
        const=True,
        help="Wait until cluster launches (takes five minutes or more) or for steps to complete"
    )
    _logging_args = [
        '-l',
        '--logging'
    ]
    _logging_kwargs = dict(
        action="store_const",
        const=True,
        help="Enable logging. Logs will be outputted to the resources folder in S3"
    )

    parser = argparse.ArgumentParser(
        prog=os.path.dirname(__file__),
        description=_fc_description,
        formatter_class=argparse.RawDescriptionHelpFormatter
    )

    subparsers = parser.add_subparsers()
    upload_resources_parser = subparsers.add_parser(
        'upload-resources',
        aliases=['u'],
        help="Upload resources for a cluster."
    )
    upload_resources_parser.set_defaults(which="upload-resources")
    upload_resources_parser.add_argument(*_config_file_args, **_config_file_kwargs)
    upload_resources_parser.add_argument(
        '-f',
        '--force-upload',
        action="store_const",
        const=True,
        help="Force upload to occur"
    )
    launch_cluster_parser = subparsers.add_parser(
        'launch-cluster',
        aliases=['l'],
        help="Launch a cluster."
    )
    launch_cluster_parser.set_defaults(which="launch-cluster")
    launch_cluster_parser.add_argument(*_config_file_args, **_config_file_kwargs)
    launch_cluster_parser.add_argument(*_wait_until_ready_args, **_wait_until_ready_kwargs)
    launch_cluster_parser.add_argument(*_logging_args, **_logging_kwargs)
    forward_local_ports_parser = subparsers.add_parser(
        'forward-local-ports',
        aliases=['f'],
        help="Forward the local ports for ssh access."
    )
    forward_local_ports_parser.set_defaults(which="forward-local-ports")
    forward_local_ports_parser.add_argument(*_cluster_id_args, **_cluster_id_kwargs)
    forward_local_ports_parser.add_argument(*_private_key_args, **_private_key_kwargs)
    forward_local_ports_parser.add_argument(*_profile_args, **_profile_kwargs)
    terminate_clusters_parser = subparsers.add_parser(
        'terminate-clusters',
        aliases=['t'],
        help="Terminate the cluster(s) that are associated with the provided cluster ID(s)"
    )
    terminate_clusters_parser.set_defaults(which="terminate-clusters")
    terminate_clusters_parser.add_argument(
        '-i',
        '--cluster-ids',
        metavar='CLUSTER_ID',
        type=str,
        nargs='+',
        help='Clusters to terminate.',
        required=True
    )
    terminate_clusters_parser.add_argument(*_profile_args, **_profile_kwargs)
    configure_streams_parser = subparsers.add_parser(
        'configure-streams',
        aliases=['c'],
        help="Push configuration for querying streams and stream archives directly"
    )
    configure_streams_parser.set_defaults(which="configure-streams")
    configure_streams_parser.add_argument(*_config_file_args, **_config_file_kwargs)
    configure_streams_parser.add_argument(*_cluster_id_args, **_cluster_id_kwargs)
    configure_streams_parser.add_argument(*_wait_until_ready_args, **_wait_until_ready_kwargs)

    wait_for_cluster_parser = subparsers.add_parser(
        'wait-for-cluster',
        aliases=['w'],
        help="Wait for the cluster to start"
    )
    wait_for_cluster_parser.set_defaults(which="wait-for-cluster")
    wait_for_cluster_parser.add_argument(*_cluster_id_args, **_cluster_id_kwargs)
    wait_for_cluster_parser.add_argument(*_profile_args, **_profile_kwargs)
    do_everything_parser = subparsers.add_parser(
        'all',
        help="Start a cluster and forward ports using configuration files"
    )
    do_everything_parser.set_defaults(which='all')
    do_everything_parser.add_argument(*_config_file_args, **_config_file_kwargs)
    do_everything_parser.add_argument(*_private_key_args, **_private_key_kwargs)
    do_everything_parser.add_argument(*_logging_args, **_logging_kwargs)
    do_everything_parser.add_argument(
        '-t',
        '--terminate-at-end',
        action="store_const",
        const=True,
        help="Terminate once ports are closed"
    )
    return parser


def load_configuration(configuration_file):
    return json.load(configuration_file)

if __name__ == "__main__":
    args = get_argument_parser().parse_args()
    config = dict()
    if hasattr(args, 'which'):
        if hasattr(args, 'config_file'):
            config = load_configuration(args.config_file)

        if args.which == "upload-resources":
            upload_resources(
                config['AWS']['S3'],
                config['AWS']['profile'],
                args.force_upload
            )
            
            upload_pump(
                config['AWS']['S3'],
                config['AWS']['profile'],
                args.force_upload
            )
        elif args.which == "launch-cluster":
            launch_emr_cluster(
                config['AWS']['S3'],
                config['AWS']['EMR'],
                config['AWS']['CloudWatch'],
                config['AWS']['profile'],
                args.wait_until_ready,
                args.logging
            )
        elif args.which == "forward-local-ports":
            forward_necessary_ports(
                args.cluster_id,
                args.private_key,
                args.profile
            )
        elif args.which == "terminate-clusters":
            terminate_emr_cluster(
                args.cluster_ids,
                args.profile
            )
        elif args.which == "configure-streams":
            configure_streams(
                args.cluster_id,
                config['AWS']['S3'],
                config['AWS']['streams'],
                config['AWS']['archives'],
                config['AWS']['profile'],
                args.wait_until_ready
            )
        elif args.which == "wait-for-cluster":
            print("Cluster can take more than 5 minutes to start...")
            wait_for_cluster(
                args.cluster_id,
                args.profile
            )
            print("Cluster ready.")
        elif args.which == "all":
            upload_resources(
                config['AWS']['S3'],
                config['AWS']['profile'],
                True
            )
            upload_pump(
                config['AWS']['S3'],
                config['AWS']['profile'],
                True
            )
            cluster_id = launch_emr_cluster(
                config['AWS']['S3'],
                config['AWS']['EMR'],
                config['AWS']['CloudWatch'],
                config['AWS']['profile'],
                True,
                args.logging
            )
            configure_streams(
                cluster_id,
                config['AWS']['S3'],
                config['AWS']['streams'],
                config['AWS']['archives'],
                config['AWS']['profile'],
                True
            )
            forward_necessary_ports(
                cluster_id,
                args.private_key,
                config['AWS']['profile']
            )
            if args.terminate_at_end:
                terminate_emr_cluster(
                    [cluster_id],
                    config['AWS']['profile']
                )

    else:
        get_argument_parser().print_help()
        exit(2)
