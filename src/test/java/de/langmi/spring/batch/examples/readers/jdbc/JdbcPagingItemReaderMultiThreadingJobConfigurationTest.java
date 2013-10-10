/**
 * Copyright 2013 Michael R. Pralow <me@michael-pralow.de>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.langmi.spring.batch.examples.readers.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * JobConfigurationTest for simple JdbcPagingItemReader.
 *
 * @author Michael R. Lange <michael.r.lange@langmi.de> 
 */
@ContextConfiguration({
    "classpath*:spring/batch/job/readers/jdbc/jdbc-paging-item-reader-multi-threading-job.xml",
    "classpath*:spring/batch/setup/**/*.xml"})
@RunWith(SpringJUnit4ClassRunner.class)
public class JdbcPagingItemReaderMultiThreadingJobConfigurationTest {

    private static final String DROP_TEST_TABLE = "DROP TABLE TEST IF EXISTS;";
    private static final String CREATE_TEST_TABLE = "CREATE TABLE TEST (ID INTEGER GENERATED BY DEFAULT AS IDENTITY, NAME VARCHAR (100))";
    private static final String INSERT = "INSERT INTO TEST (NAME) VALUES (?)";
    /** Logger. */
    private final Logger LOG = LoggerFactory.getLogger(getClass());
    /** Lines count from input file. */
    private static final int EXPECTED_COUNT = 10000;
    /** JobLauncherTestUtils Bean. */
    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;
    @Autowired
    private DataSource dataSource;

    /** Launch Test. */
    @Test
    public void launchJob() throws Exception {
        // Job parameters
        Map<String, JobParameter> jobParametersMap = new HashMap<String, JobParameter>();
        jobParametersMap.put("time", new JobParameter(System.currentTimeMillis()));
        jobParametersMap.put("output.file", new JobParameter("file:target/test-outputs/readers/jdbc/jdbc-paging-item-reader-multi-threading/output.txt"));

        // launch the job
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(new JobParameters(jobParametersMap));

        // assert job run status
        assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());

        // output step summaries
        for (StepExecution step : jobExecution.getStepExecutions()) {
            LOG.debug(step.getSummary());
            assertEquals("Read Count mismatch, changed input?",
                         EXPECTED_COUNT, step.getReadCount());
        }
    }

    /**
     * Setup Datasource and create table for test.
     *
     * @throws Exception 
     */
    @Before
    public void setUp() throws Exception {
        
        // drop table if exists
        Connection conn = dataSource.getConnection();
        Statement st = conn.createStatement();
        st.execute(DROP_TEST_TABLE);
        conn.commit();
        st.close();
        conn.close();

        // create table
        conn = dataSource.getConnection();
        st = conn.createStatement();
        st.execute(CREATE_TEST_TABLE);
        conn.commit();
        st.close();
        conn.close();

        // fill with values
        conn = dataSource.getConnection();
        // prevent auto commit for batching
        conn.setAutoCommit(false);
        PreparedStatement ps = conn.prepareStatement(INSERT);
        // fill with values
        for (int i = 0; i < EXPECTED_COUNT; i++) {
            ps.setString(1, String.valueOf(i));
            ps.addBatch();
        }
        ps.executeBatch();
        conn.commit();
        ps.close();
        conn.close();
    }
}
