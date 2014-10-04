<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@taglib uri="http://www.springframework.org/tags/form" prefix="form"%>
<%@taglib uri="http://www.springframework.org/tags" prefix="spring"%>
<html>
<head>
<title>quote</title>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
</head>
<body style="font-size: 16px;">	
	<table>
		<tr>
			<td><a href="/forum/list/MUSIC/1">Music</a><td>
			<td><a href="/forum/list/MOVIE/1">Movie</a><td>
		</td>
	</table>

	<table style="font-size: 10px;">
	<c:forEach var="c" items="${contents}">       
		<tr>			
			<td><a target="_blank" href="http://${c.url}"><c:out value="${c.title}" /></a></td>
			<td><fmt:formatDate pattern="dd-MM-yyyy" value="${c.createdDate}" /></td>
			<td><c:out value="${c.source}" /></td>
		</tr>
	</c:forEach>
	</table>	
</body>
</html>
