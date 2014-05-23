/* @java.file.header */

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.kernal.processors.hadoop.v2;

import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.reduce.*;
import org.gridgain.grid.*;
import org.gridgain.grid.hadoop.*;
import org.gridgain.grid.util.typedef.internal.*;

/**
 * Hadoop combine task implementation for v2 API.
 */
public class GridHadoopV2CombineTask extends GridHadoopTask {
    /**
     * @param taskInfo Task info.
     */
    public GridHadoopV2CombineTask(GridHadoopTaskInfo taskInfo) {
        super(taskInfo);
    }

    /** {@inheritDoc} */
    @SuppressWarnings({"ConstantConditions", "unchecked"})
    @Override public void run(GridHadoopTaskContext taskCtx) throws GridException {
        GridHadoopV2Job jobImpl = (GridHadoopV2Job)taskCtx.job();

        JobContext jobCtx = jobImpl.hadoopJobContext();

        try {
            Reducer combiner = U.newInstance(jobCtx.getCombinerClass());

            GridHadoopV2Context hadoopCtx = new GridHadoopV2Context(jobCtx.getConfiguration(), taskCtx,
                jobImpl.attemptId(info()));

            if (jobImpl.reducers() == 0) {
                // No reducers defined, so set correct writer to combiner.
                OutputFormat outputFormat = U.newInstance(jobCtx.getOutputFormatClass());

                RecordWriter writer = outputFormat.getRecordWriter(hadoopCtx);

                hadoopCtx.writer(writer);

                try {
                    combiner.run(new WrappedReducer().getReducerContext(hadoopCtx));
                }
                finally {
                    writer.close(hadoopCtx);
                }

                OutputCommitter outputCommitter = outputFormat.getOutputCommitter(hadoopCtx);

                outputCommitter.commitTask(hadoopCtx);
            }
            else
                combiner.run(new WrappedReducer().getReducerContext(hadoopCtx));
        }
        catch (InterruptedException e) {
            throw new GridInterruptedException(e);
        }
        catch (Exception e) {
            throw new GridException(e);
        }
    }
}
