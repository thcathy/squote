<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html>
<head>
	<title>quote</title>
	<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
	<script src="//ajax.googleapis.com/ajax/libs/jquery/2.0.3/jquery.min.js"></script>
</head>
<body>	
	<table style="font-size: 10px;" id="quotes">	
		<tr>
			<td>Code</td>
			<td>Price</td>
			<td>Change</td>
			<td>1 year %</td>
			<td>2 year %</td>
			<td>3 year %</td>
			<td>Year Low - High</td>
			<td>PE</td>
			<td>Yield</td>
			<td>NAV</td>
		</tr>
	</table>
	<div id="status">Loading</div>
	<script type="text/javascript">	
		var retry=0
		var retryLimit=20
	
		function resubmit(jqXHR, status, errorThrown){
			if (status == 'timeout') poll()
		}

		function displayResult(data) {
			$("#status").html('')
			
			hceiRow = '<tr>'
			hceiRow += '<td>' + data.hcei.stockCode + '</td>'
			hceiRow += '<td>' + data.hcei.price + '</td>'
			hceiRow += '<td>' + data.hcei.change + '</td>'
			hceiRow += '<td>' + data.hcei.lastYearPercentage.toFixed(2) + '</td>'
			hceiRow += '<td>' + data.hcei.last2YearPercentage.toFixed(2) + '</td>'
			hceiRow += '<td>' + data.hcei.last3YearPercentage.toFixed(2) + '</td>'
			hceiRow += '<td>' + data.hcei.yearLow + '-' + data.hcei.yearHigh + '</td>'
			hceiRow += '</tr>'
			$(hceiRow).appendTo("#quotes")	
			
			$.each(data.quotes, function(i,quote){
				content = '<tr>'
				content += '<td>' + quote.stockCode + '</td>'
				content += '<td>' + quote.price + '</td>'
				content += '<td>' + quote.change + '</td>'
				content += '<td>' + (Number(quote.lastYearPercentage) - Number(data.hcei.lastYearPercentage)).toFixed(2) + '</td>'
				content += '<td>' + (Number(quote.last2YearPercentage) - Number(data.hcei.last2YearPercentage)).toFixed(2) + '</td>'
				content += '<td>' + (Number(quote.last3YearPercentage) - Number(data.hcei.last3YearPercentage)).toFixed(2) + '</td>'
				content += '<td>' + quote.yearLow + '-' + quote.yearHigh + '</td>'
				content += '<td>' + quote.pe + '</td>'
				content += '<td>' + quote.yield + '</td>'
				content += '<td>' + quote.nav + '</td>'
				content += '</tr>'
				$(content).appendTo("#quotes")				
			});
		}

		// call for data
		(function poll(){
			retry++
			if (retry > retryLimit) return
			
			$("#status").html($("#status").html() + '...')
		    $.ajax({ url: "liststocksperf", success: displayResult, dataType: "json", 
			    error: poll, 
			    timeout: 20000 })
		})()
	</script>
</body>
</html>