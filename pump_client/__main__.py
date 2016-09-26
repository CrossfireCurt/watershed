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
import os

from pump_client.PumpClient import PumpClient

def get_argument_parser():
    _fc_description = "Client to manage and monitor Pump."

    _query_args = ["-q", "--query"]
    _query_kwargs = dict(action="store", help="Specify the query Pump will use to pull records out of Drill.", required=True, type=str)

    _stream_args = ["-s", "--stream"]
    _stream_kwargs = dict(action="store", help="Specify the Kinesis stream that Pump will emit records to.", required=True, type=str)

    _num_records_args = ["-n", "--num-records"]
    _num_records_kwargs = dict(action="store", help="Specify the number of records to return in the preview.", required=True, type=int)

    _job_id_args = ["-id", "--job-id"]
    _job_id_kwargs = dict(action="store", help="Specify the Job ID to retrieve.", required=True, type=str)

    _show_progress_args = ["-p", "--show-progress"]
    _show_progress_kwargs = dict(action="store_const", const=True, required=False, help="Poll Pump for progress of job")

    _summary_only_args = ["-t", "--summary-only"]
    _summary_only_kwargs = dict(action="store_true", required=False, help="Only show summary information about the Job")

    _replay_flag_args = ["-r", "--replay"]
    _replay_flag_kargs = dict(action="store_true", required=False, help="Add replay flag to documents")

    _overwrite_flag_args = ["-o", "--overwrite"]
    _overwrite_flag_kargs = dict(action="store_true", required=False, help="Add overwrite flag to documents")


    parser = argparse.ArgumentParser(
        prog=os.path.dirname(__file__),
        description=_fc_description,
        formatter_class=argparse.RawDescriptionHelpFormatter
    )

    subparsers = parser.add_subparsers()

    create_job_parser = subparsers.add_parser('create-job', help="Enqueue a job for Pump to work on.")
    create_job_parser.set_defaults(which="create-job")
    create_job_parser.add_argument(*_query_args, **_query_kwargs)
    create_job_parser.add_argument(*_stream_args, **_stream_kwargs)
    create_job_parser.add_argument(*_show_progress_args, **_show_progress_kwargs)
    create_job_parser.add_argument(*_replay_flag_args, **_replay_flag_kargs)
    create_job_parser.add_argument(*_overwrite_flag_args, **_overwrite_flag_kargs)

    preview_job_parser = subparsers.add_parser('preview-job', help="Preview a job before Pump runs it.")
    preview_job_parser.set_defaults(which="preview-job")
    preview_job_parser.add_argument(*_query_args, **_query_kwargs)
    preview_job_parser.add_argument(*_num_records_args, **_num_records_kwargs)

    get_job_parser = subparsers.add_parser('get-job', help="Retreive Job status from Pump.")
    get_job_parser.set_defaults(which="get-job")
    get_job_parser.add_argument(*_job_id_args, **_job_id_kwargs)
    get_job_parser.add_argument(*_show_progress_args, **_show_progress_kwargs)
    get_job_parser.add_argument(*_summary_only_args, **_summary_only_kwargs)

    get_all_jobs_parser = subparsers.add_parser('get-all-jobs', help="Retreive all Jobs and their statuses from Pump.")
    get_all_jobs_parser.set_defaults(which="get-all-jobs")    
    get_all_jobs_parser.add_argument(*_summary_only_args, **_summary_only_kwargs)

    return parser

if __name__ == "__main__":
    parser = get_argument_parser()
    args = parser.parse_args()
    if(not vars(args)):
        parser.print_help()
        parser.exit(1)
        
    pumpClient = PumpClient('http://localhost:8080/pump')
    
    config = dict()
    if hasattr(args, 'which'):
        if args.which == "create-job":
            pumpClient.create_job(args.query, args.stream, args.show_progress, args.replay, args.overwrite)
              
        elif args.which == "get-job":
            pumpClient.get_job(args.job_id, args.show_progress, args.summary_only)
        
        elif args.which == "get-all-jobs":
            pumpClient.get_all_jobs(args.summary_only)
        
        elif args.which == "preview-job":
            pumpClient.preview_job(args.query, args.num_records)
        
        else:
            parser.print_help()
            exit(2)
            
    else:
        parser.print_help()
        exit(2)
        