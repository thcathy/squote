<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@taglib uri="http://www.springframework.org/tags/form" prefix="form"%>
<%@taglib uri="http://www.springframework.org/tags" prefix="spring"%>
<html>
<head>
<title>quote</title>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
</head>
<body>
	<form>
		<input type="text"  style="width: 40px;" name="action" /><input type="submit" /><input type="text" style="width: 90%" name="codeList" value="${codeList}" />
	</form>
			
	<table style="font-size: 10px;">
		<c:forEach var="q" items="${indexes}">
			<tr>
				<td>${q.stockCode}</td>
				<td>${q.price}</td>
				<td>${q.change}</td>
				<td>${q.low}-${q.high}</td>
			</tr>
		</c:forEach>
	</table>
	<table style="font-size: 10px;">	
		<c:forEach var="q" items="${quotes}">       
			<tr>
				<td>${q.stockCode}</td>
				<td>${q.price}</td>
				<td>${q.changeAmount}</td>
				<td>${q.change}</td>
				<td>${q.low}-${q.high}</td>
				<td>${q.lastUpdate}</td>
			</tr>
		</c:forEach>		
	</table>	
	<br/>
	<table style="font-size: 10px;">	
		<c:forEach items="${holdingMap}" var="entry">
			<tr>
				<td>${entry.key.code}</td>
				<td><fmt:formatNumber value="${(hsce.priceDoubleValue - entry.key.hsce.doubleValue())/entry.key.hsce.doubleValue()*100}" type="number" maxFractionDigits="2" />%</td>
				<td>${entry.key.side}</td>
				<td><fmt:formatDate pattern="yyyy-MM-dd" value="${entry.key.date}" /></td>							
				<td>${entry.key.quantity}</td>
				<td><fmt:formatNumber value="${entry.key.price}" type="number" maxFractionDigits="2" /></td>
				<td>${entry.key.gross}</td>
				<td><fmt:formatNumber value="${(entry.value.priceDoubleValue - entry.key.price.doubleValue())/entry.key.price.doubleValue()*100}" type="number" maxFractionDigits="2" />%</td>
											
			</tr>
		</c:forEach>
		<tr>
			<td><a href="${pageContext.request.contextPath}/holdingstocks">Manage holding stock</a></td>
		</tr>
	</table>
	<br/>
	<table style="font-size: 10px;" cellspacing="4">
		<thead>
			<tr>
				<td>day before</td>
				<td>date</td>
				<td>indebtedness</td>
				<td>notes</td>
				<td>closingBalance</td>
				<td>exchangeFund</td>
				<td>total</td>
				<td>change</td>
				<td>%</td>
				<td>HSI</td>
				<td>pe</td>
				<td>yield</td>
				<td>HSCE</td>
				<td>PE</td>
				<td>yield</td>
			</tr>
		</thead>
		<tr>
			<td>Today</td>
			<td>${tbase.date}</td>
			<td>${tbase.moneyBase.indebtedness}</td>
			<td>${tbase.moneyBase.notes}</td>
			<td>${tbase.moneyBase.closingBalance}</td>
			<td>${tbase.moneyBase.exchangeFund}</td>
			<td>${tbase.moneyBase.total}</td>
			<td></td>
			<td></td>
			<td><fmt:formatNumber value="${tbase.hsi.price}" type="number" maxFractionDigits="2" /></td>
			<td>${tbase.hsi.pe}</td>
			<td>${tbase.hsi.yield}</td>
			<td><fmt:formatNumber value="${tbase.hscei.price}" type="number" maxFractionDigits="2" /></td>
			<td>${tbase.hscei.pe}</td>
			<td>${tbase.hscei.yield}</td>
		</tr>
		<tr>
			<td>T-1</td>
			<td>${tminus1.date}</td>
			<td>${tminus1.moneyBase.indebtedness}</td>
			<td>${tminus1.moneyBase.notes}</td>
			<td>${tminus1.moneyBase.closingBalance}</td>
			<td>${tminus1.moneyBase.exchangeFund}</td>
			<td>${tminus1.moneyBase.total}</td>
			<td>${tbase.moneyBase.total - tminus1.moneyBase.total}</td>
			<td><fmt:formatNumber value="${(tbase.moneyBase.total - tminus1.moneyBase.total)/tbase.moneyBase.total*100}" type="number" maxFractionDigits="2" /></td>
			<td><fmt:formatNumber value="${tminus1.hsi.price}" type="number" maxFractionDigits="2" /></td>
			<td>${tminus1.hsi.pe}</td>
			<td>${tminus1.hsi.yield}</td>
			<td><fmt:formatNumber value="${tminus1.hscei.price}" type="number" maxFractionDigits="2" /></td>
			<td>${tminus1.hscei.pe}</td>
			<td>${tminus1.hscei.yield}</td>
		</tr>
		<tr>
			<td>T-7</td>
			<td>${tminus7.date}</td>
			<td>${tminus7.moneyBase.indebtedness}</td>
			<td>${tminus7.moneyBase.notes}</td>
			<td>${tminus7.moneyBase.closingBalance}</td>
			<td>${tminus7.moneyBase.exchangeFund}</td>
			<td>${tminus7.moneyBase.total}</td>
			<td>${tbase.moneyBase.total - tminus7.moneyBase.total}</td>
			<td><fmt:formatNumber value="${(tbase.moneyBase.total - tminus7.moneyBase.total)/tbase.moneyBase.total*100}" type="number" maxFractionDigits="2" /></td>
			<td><fmt:formatNumber value="${tminus7.hsi.price}" type="number" maxFractionDigits="2" /></td>
			<td>${tminus7.hsi.pe}</td>
			<td>${tminus7.hsi.yield}</td>
			<td><fmt:formatNumber value="${tminus7.hscei.price}" type="number" maxFractionDigits="2" /></td>
			<td>${tminus7.hscei.pe}</td>
			<td>${tminus7.hscei.yield}</td>
		</tr>
		<tr>
			<td>T-30</td>
			<td>${tminus30.date}</td>
			<td>${tminus30.moneyBase.indebtedness}</td>
			<td>${tminus30.moneyBase.notes}</td>
			<td>${tminus30.moneyBase.closingBalance}</td>
			<td>${tminus30.moneyBase.exchangeFund}</td>
			<td>${tminus30.moneyBase.total}</td>
			<td>${tbase.moneyBase.total - tminus30.moneyBase.total}</td>
			<td><fmt:formatNumber value="${(tbase.moneyBase.total - tminus30.moneyBase.total)/tbase.moneyBase.total*100}" type="number" maxFractionDigits="2" /></td>
			<td><fmt:formatNumber value="${tminus30.hsi.price}" type="number" maxFractionDigits="2" /></td>
			<td>${tminus30.hsi.pe}</td>
			<td>${tminus30.hsi.yield}</td>
			<td><fmt:formatNumber value="${tminus30.hscei.price}" type="number" maxFractionDigits="2" /></td>
			<td>${tminus30.hscei.pe}</td>
			<td>${tminus30.hscei.yield}</td>
		</tr>
		<tr>
			<td>T-60</td>
			<td>${tminus60.date}</td>
			<td>${tminus60.moneyBase.indebtedness}</td>
			<td>${tminus60.moneyBase.notes}</td>
			<td>${tminus60.moneyBase.closingBalance}</td>
			<td>${tminus60.moneyBase.exchangeFund}</td>
			<td>${tminus60.moneyBase.total}</td>
			<td>${tbase.moneyBase.total - tminus60.moneyBase.total}</td>
			<td><!--<fmt:formatNumber value="${(tbase.moneyBase.total - tminus60.moneyBase.total)/tbase.moneyBase.total*100}" type="number" maxFractionDigits="2" />--></td>
			<td>${tminus60.hsi.price}</td>
			<td>${tminus60.hsi.pe}</td>
			<td>${tminus60.hsi.yield}</td>
			<td>${tminus60.hscei.price}</td>
			<td>${tminus60.hscei.pe}</td>
			<td>${tminus60.hscei.yield}</td>
		</tr>

	</table>	
</body>
</html>
