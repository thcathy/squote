<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@taglib uri="http://www.springframework.org/tags/form" prefix="form"%>
<%@taglib uri="http://www.springframework.org/tags" prefix="spring"%>
<html>
<head>
<title>squote</title>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
</head>
<body style="font-size: 16px;">
	Forum:	
	<table>
		<tr>			
			<td><a href="${pageContext.request.contextPath}/forum/list/MUSIC/1">Music</a><td>
			<td><a href="${pageContext.request.contextPath}/forum/list/MOVIE/1">Movie</a><td>
		</td>
	</table>

	Quote:
	<table cellpadding="2">
		<tr>
			<td><a href="${pageContext.request.contextPath}/quote/list">List</a></td>
			<td><a href="${pageContext.request.contextPath}/quote/single/2828">Single</a></td>
			<td><a href="${pageContext.request.contextPath}/quote/stocksperf">Indexes Constituents Performance</a></td>
		</tr>		
	</table>
	
	DB Manage:
	<table cellpadding="2">
		<tr>
			<td><a href="${pageContext.request.contextPath}/holdingstocks">Holding stocks</a></td>
			<td><a href="${pageContext.request.contextPath}/quote/createholdingstock">Create Holding stocks by execution msg</a></td>
			<td><a href="${pageContext.request.contextPath}/marketdailyreports">Market daily reports</a></td>
		</tr>		
	</table>
</body>
</html>
