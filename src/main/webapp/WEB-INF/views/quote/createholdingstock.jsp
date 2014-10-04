<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt"%>
<%@taglib uri="http://www.springframework.org/tags/form" prefix="form"%>
<%@taglib uri="http://www.springframework.org/tags" prefix="spring"%>
<html>
<head>
<title>quote</title>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
</head>
<body>
	<form method="post">
		<textarea id="message" name="message" rows="5" cols="30">${message}</textarea><br />
		hscei:<input type="text"  style="width: 40px;" name="hscei" /><br />
		<input type="submit" />
	</form>
	<p>${resultMessage}</p>
	<p>${holdingStock}</p>
	<p><a href="/holdingstocks">Holding stocks</a></p>	
</body>
</html>
