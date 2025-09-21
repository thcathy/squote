package squote.domain;

import java.math.BigDecimal;

public record FlowSummaryInfo(
    String clearingDate,
    String settlementDate,
    String currency,
    String cashFlowType,
    String cashFlowDirection,
    BigDecimal cashFlowAmount,
    String cashFlowRemark,
    String cashFlowID
) {}
