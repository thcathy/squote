package squote.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import squote.domain.Fund;
import squote.domain.FundHolding;
import squote.domain.repository.FundRepository;

import java.math.BigDecimal;
import java.util.*;

import static java.util.stream.Collectors.toMap;

@Service
public class SplitInterestService {
	protected final Logger log = LoggerFactory.getLogger(getClass());

	final FundRepository fundRepo;

	public SplitInterestService(FundRepository fundRepo) {
		this.fundRepo = fundRepo;
	}

	public List<String> splitInterest(String userId, String code, String amount, String[] fundNames) {
		var results = new ArrayList<String>();
		var funds = getFunds(userId, fundNames);
		if (funds.size() < fundNames.length) {
			return List.of("Error: cannot find fund=" + Arrays.toString(fundNames));
		}

		var fundNameToHoldingBeforeChange = getHoldings(code, funds);
		if (fundNameToHoldingBeforeChange.size() < fundNames.length) {
			return List.of("Error: cannot find code=%s in all funds=%s".formatted(code, Arrays.toString(fundNames)));
		}

		var totalQuantity = fundNameToHoldingBeforeChange.values().stream().mapToInt(h -> h.getQuantity().intValue()).sum();
		double amountDouble = 0;
		try {
			amountDouble = Double.parseDouble(amount);
		} catch (NumberFormatException e) {
			return List.of("Error: invalid amount=" + amount);
		}
		var amountPerQuantity =  amountDouble / totalQuantity;

		funds.forEach(f -> {
			var holdingBefore = fundNameToHoldingBeforeChange.get(f.name);
			f.payInterest(code, getInterest(holdingBefore, amountPerQuantity));
			fundRepo.save(f);
			results.add(resultString(f, holdingBefore, f.getHoldings().get(code)));
		});

		log.debug("Return results: {}", results);
		return results;
	}

	private static Map<String, FundHolding> getHoldings(String code, List<Fund> funds) {
		return funds.stream()
				.filter(f -> f.getHoldings().containsKey(code))
				.collect(toMap(f -> f.name, f -> f.getHoldings().get(code)));
	}

	private List<Fund> getFunds(String userId, String[] fundNames) {
		return Arrays.stream(fundNames)
				.map(name -> fundRepo.findByUserIdAndName(userId, name))
				.filter(Optional::isPresent)
				.map(Optional::get).toList();
	}

	private String resultString(Fund fund, FundHolding holdingBefore, FundHolding holdingAfter) {
		return "[%s] %s: %s, %.2f > %.2f, %.2f > %.2f".formatted(
				fund.name, holdingBefore.getCode(), holdingAfter.getQuantity(),
				holdingBefore.getGross(), holdingAfter.getGross(),
				holdingBefore.getPrice(), holdingAfter.getPrice());
	}

	private BigDecimal getInterest(FundHolding holding, double amountPerQuantity) {
		return holding.getQuantity().multiply(BigDecimal.valueOf(amountPerQuantity));
	}
}
    
