/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.presto.pinot;

import com.facebook.presto.pinot.query.PinotQueryGenerator;
import com.facebook.presto.spi.ConnectorSplit;
import com.facebook.presto.spi.HostAddress;
import com.facebook.presto.spi.schedule.NodeSelectionStrategy;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Optional;

import static com.facebook.presto.spi.schedule.NodeSelectionStrategy.NO_PREFERENCE;
import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

public class PinotSplit
        implements ConnectorSplit
{
    private final String connectorId;
    private final SplitType splitType;

    // Properties needed for broker split type
    private final Optional<PinotQueryGenerator.GeneratedPql> brokerPql;

    // Properties needed for segment split type
    private final Optional<String> segmentPql;
    private final List<String> segments;
    private final Optional<String> segmentHost;

    @JsonCreator
    public PinotSplit(
            @JsonProperty("connectorId") String connectorId,
            @JsonProperty("splitType") SplitType splitType,
            @JsonProperty("brokerPql") Optional<PinotQueryGenerator.GeneratedPql> brokerPql,
            @JsonProperty("segmentPql") Optional<String> segmentPql,
            @JsonProperty("segments") List<String> segments,
            @JsonProperty("segmentHost") Optional<String> segmentHost)
    {
        this.connectorId = requireNonNull(connectorId, "connector id is null");
        this.splitType = requireNonNull(splitType, "splitType id is null");
        this.brokerPql = requireNonNull(brokerPql, "brokerPql is null");
        this.segmentPql = requireNonNull(segmentPql, "table name is null");
        this.segments = ImmutableList.copyOf(requireNonNull(segments, "segment is null"));
        this.segmentHost = requireNonNull(segmentHost, "host is null");

        // make sure the segment properties are present when the split type is segment
        if (splitType == SplitType.SEGMENT) {
            checkArgument(segmentPql.isPresent(), "Table name is missing from the split");
            checkArgument(!segments.isEmpty(), "Segments are missing from the split");
            checkArgument(segmentHost.isPresent(), "Segment host address is missing from the split");
        }
        else {
            checkArgument(brokerPql.isPresent(), "brokerPql is missing from the split");
        }
    }

    public static PinotSplit createBrokerSplit(String connectorId, PinotQueryGenerator.GeneratedPql brokerPql)
    {
        return new PinotSplit(
                requireNonNull(connectorId, "connector id is null"),
                SplitType.BROKER,
                Optional.of(requireNonNull(brokerPql, "brokerPql is null")),
                Optional.empty(),
                ImmutableList.of(),
                Optional.empty());
    }

    public static PinotSplit createSegmentSplit(String connectorId, String pql, List<String> segments, String segmentHost)
    {
        return new PinotSplit(
                requireNonNull(connectorId, "connector id is null"),
                SplitType.SEGMENT,
                Optional.empty(),
                Optional.of(requireNonNull(pql, "pql is null")),
                requireNonNull(segments, "segments are null"),
                Optional.of(requireNonNull(segmentHost, "segmentHost is null")));
    }

    @JsonProperty
    public String getConnectorId()
    {
        return connectorId;
    }

    @JsonProperty
    public SplitType getSplitType()
    {
        return splitType;
    }

    @JsonProperty
    public Optional<PinotQueryGenerator.GeneratedPql> getBrokerPql()
    {
        return brokerPql;
    }

    @JsonProperty
    public Optional<String> getSegmentPql()
    {
        return segmentPql;
    }

    @JsonProperty
    public Optional<String> getSegmentHost()
    {
        return segmentHost;
    }

    @JsonProperty
    public List<String> getSegments()
    {
        return segments;
    }

    @Override
    public String toString()
    {
        return toStringHelper(this)
                .add("connectorId", connectorId)
                .add("splitType", splitType)
                .add("segmentPql", segmentPql)
                .add("brokerPql", brokerPql)
                .add("segments", segments)
                .add("segmentHost", segmentHost)
                .toString();
    }

    @Override
    public NodeSelectionStrategy getNodeSelectionStrategy()
    {
        return NO_PREFERENCE;
    }

    @Override
    public List<HostAddress> getPreferredNodes(List<HostAddress> sortedCandidates)
    {
        return ImmutableList.of();
    }

    @Override
    public Object getInfo()
    {
        return this;
    }

    public enum SplitType
    {
        SEGMENT,
        BROKER,
    }
}
