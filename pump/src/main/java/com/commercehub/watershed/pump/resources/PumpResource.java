package com.commercehub.watershed.pump.resources;

import com.commercehub.watershed.pump.model.Job;
import com.commercehub.watershed.pump.model.JobPreview;
import com.commercehub.watershed.pump.model.PreviewSettings;
import com.commercehub.watershed.pump.model.PumpSettings;
import com.commercehub.watershed.pump.service.JobService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;

/**
 * REST endpoints to query, start, and manage Jobs
 */
@Path("/jobs")
@Produces(MediaType.APPLICATION_JSON)
public class PumpResource {

    @Inject
    private ObjectMapper objectMapper;

    @Inject
    private JobService jobService;

    /**
     * GET Job in its current state
     *
     * @param jobId
     * @return Response
     * @throws IOException
     */
    @Path("/{job_id}")
    @GET
    public Response getJob(@PathParam("job_id") String jobId) throws IOException{
        Job job = jobService.getJob(jobId);
        if(job == null){
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        String response = objectMapper.writeValueAsString(job);
        return Response.ok().entity(response).build();
    }

    /**
     * GET all Jobs in their current state
     *
     * @return Response
     * @throws IOException
     */
    @GET
    public Response getAllJobs() throws IOException{
        Collection<Job> jobs = jobService.getAllJobs();

        String response = objectMapper.writeValueAsString(jobs);
        return Response.ok().entity(response).build();
    }

    /**
     * POST PumpSettings to kickoff a Job
     *
     * @param pumpSettings
     * @return Response
     * @throws IOException
     */
    @POST
    public Response enqueueJob(@Valid PumpSettings pumpSettings) throws IOException{
        Job job = jobService.enqueueJob(pumpSettings);

        String response = objectMapper.writeValueAsString(job);
        return Response.ok().entity(response).build();
    }

    /**
     * POST PreviewSettings to retrieve a preview of a query
     *
     * @param previewSettings
     * @return Response
     * @throws IOException
     * @throws SQLException
     */
    @Path("/preview")
    @POST
    public Response previewJob(@Valid PreviewSettings previewSettings) throws IOException, SQLException{
        JobPreview jobPreview = jobService.getJobPreview(previewSettings);

        String response = objectMapper.writeValueAsString(jobPreview);
        return Response.ok().entity(response).build();
    }
}
