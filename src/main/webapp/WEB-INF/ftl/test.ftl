<#import "/spring.ftl" as spring/>

<html>
	<body>
		<@spring.message "author"/>:
		I am an ftl!
		${task.getName()}
	</body>
</html>