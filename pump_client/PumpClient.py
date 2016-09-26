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

import requests
import json
import time

class PumpClient:    
    def __init__(self, base_url):
        self._base_url = base_url
    
    def get_all_jobs(self, summary_only):
        print('Showing all jobs...')

        url = self._base_url + '/jobs'
        jobs = []
        try:
            r = requests.get(url)
            jobs = json.loads(r.text)
        except requests.exceptions.RequestException as e: 
            print(e)

        for job in jobs:
            self.print_job(job, summary_only)
        return


    def get_job(self, job_id, poll_for_progress, summary_only):        
        print('Showing job...')
        print('Job: {}'.format(job_id))

        url = self._base_url + '/jobs/' + job_id
        job = None

        while True:
            job = dict()

            try:
                r = requests.get(url)
                print(r)
                job = json.loads(r.text)
            except requests.exceptions.RequestException as e: 
                print(e)
                raise

            if("jobId" not in job):
                raise Exception('Invalid job.')

            self.print_job(job, summary_only)

            if(not poll_for_progress or job["stage"] == "COMPLETED_ERROR" or job["stage"] == "COMPLETED_SUCCESS"):
                break

            time.sleep(1)

        return job


    def print_job(self, job, summary_only):
        if(summary_only):        
            print('{jobId}: {stage}({elapsedTime}, {meanRate}) {successCount} successful, {failureCount} failed, {pendingCount} pending'.format(
                    jobId=job["jobId"],
                    stage=job["stage"],
                    elapsedTime=job["elapsedTimePretty"],
                    meanRate=job["meanRatePretty"],
                    successCount=job["successfulRecordCount"],
                    failureCount=job["failureRecordCount"],
                    pendingCount=job["pendingRecordCount"]
                ))
        else:
            print(json.dumps(job, sort_keys=True, indent=4, separators=(',', ': ')))


    def create_job(self, query_in, stream_out, poll_for_progress, replay_flag, overwrite_flag):
        print('Creating job...')
        print('Query: {}'.format(query_in))
        print('Stream: {}'.format(stream_out))
        print('Polling?: {}'.format(poll_for_progress))

        url = self._base_url + '/jobs'
        payload = {
            'queryIn': query_in,
            'streamOut': stream_out,
            'hasReplayFlag': replay_flag,
            'hasOverwriteFlag': overwrite_flag
        }
        job = dict()
        try:
            r = requests.post(url, data=json.dumps(payload), headers={'content-type': 'application/json'})
            job = json.loads(r.text)
        except requests.exceptions.RequestException as e: 
            print(e)
            raise

        if(not poll_for_progress):
            self.print_job(job, True)
            return job

        if(not 'jobId' in job):
            return job

        print('Submitted job ID={}'.format(job["jobId"]))
        self.get_job(job['jobId'], poll_for_progress, True)
        return job

    def preview_job(self, query_in, num_records):
        print('Previewing job...')
        print('Query: {}'.format(query_in))
        print('Num records: {}'.format(num_records))

        url = self._base_url + '/jobs/preview'
        jobPreview = dict()
        try:
            payload = { 'queryIn': query_in, 'previewCount': num_records }
            r = requests.post(url, data=json.dumps(payload), headers={'content-type': 'application/json'})
            print(r)
            jobPreview = json.loads(r.text)
        except requests.exceptions.RequestException as e: 
            print(e)
            raise

        print('Row preview:')
        for row in jobPreview["rows"]:
            print(row)

        print('Total number of rows: {}'.format(jobPreview["count"]))
        return jobPreview
