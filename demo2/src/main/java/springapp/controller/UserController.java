package springapp.controller;


import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import springapp.model.User;
import springapp.repository.UserRespository;



@RestController
public class UserController {

	private  final UserRespository userRespository;
	
	public UserController(UserRespository userRespository) {
		this.userRespository = userRespository;
	}

	
	@PostMapping("addUser")
	public User saveUser(@RequestBody User  user) {
		User save = userRespository.save(user);
		return save;
		
	}
}

