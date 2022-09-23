/*
 * Copyright (c) 2016 Memorial Sloan-Kettering Cancer Center.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF MERCHANTABILITY OR FITNESS
 * FOR A PARTICULAR PURPOSE. The software and documentation provided hereunder
 * is on an "as is" basis, and Memorial Sloan-Kettering Cancer Center has no
 * obligations to provide maintenance, support, updates, enhancements or
 * modifications. In no event shall Memorial Sloan-Kettering Cancer Center be
 * liable to any party for direct, indirect, special, incidental or
 * consequential damages, including lost profits, arising out of the use of this
 * software and its documentation, even if Memorial Sloan-Kettering Cancer
 * Center has been advised of the possibility of such damage.
 */

/*
 * This file is part of cBioPortal CMO-Pipelines.
 *
 * cBioPortal is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.cbioportal.annotation.pipeline;

import org.apache.commons.lang.StringUtils;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamWriter;
import org.springframework.batch.item.file.FlatFileHeaderCallback;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.transform.PassThroughLineAggregator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Zachary Heins
 */
public class MutationRecordWriter implements ItemStreamWriter<String> {

    @Value("#{jobParameters[outputFilename]}")
    private String outputFilename;

    @Value("#{jobParameters[sortOutput]}")
    private boolean sortOutput;

    @Value("#{stepExecutionContext['commentLines']}")
    private List<String> commentLines;

    @Value("#{stepExecutionContext['mutation_header']}")
    private List<String> header;

    @Value("#{stepExecutionContext['records_to_write_count']}")
    private Integer recordsToWriteCount;

    private Path stagingFile;
    private FlatFileItemWriter<String> flatFileItemWriter = new FlatFileItemWriter<>();

    // Set up the writer and print the json from CVR to a file
    @Override
    public void open(ExecutionContext ec) throws ItemStreamException {
        if (recordsToWriteCount > 0) {
            stagingFile = Paths.get(outputFilename);

            PassThroughLineAggregator aggr = new PassThroughLineAggregator();
            flatFileItemWriter.setLineAggregator(aggr);
            flatFileItemWriter.setResource(new FileSystemResource(stagingFile.toString()));
            flatFileItemWriter.setHeaderCallback(new FlatFileHeaderCallback() {
                @Override
                public void writeHeader(Writer writer) throws IOException {
                    // first write out the comment lines, then write the actual header
                    for (String comment : commentLines) {
                        writer.write(comment + "\n");
                    }
                    writer.write(StringUtils.join(header, "\t"));
                }
            });
            flatFileItemWriter.open(ec);
        }
    }

    @Override
    public void update(ExecutionContext ec) throws ItemStreamException {
    }

    @Override
    public void close() throws ItemStreamException {
        if (recordsToWriteCount > 0) {
            flatFileItemWriter.close();
        }
    }

    @Override
    public void write(List<? extends String> items) throws Exception {
        if (sortOutput) {
            List<? extends String> tmp = new ArrayList<>(items);
            Collections.sort(tmp);
            items = tmp;
        }
        if (recordsToWriteCount > 0) {
            flatFileItemWriter.write(items);
        }

    }
}
