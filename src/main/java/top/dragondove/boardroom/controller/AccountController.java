package top.dragondove.boardroom.controller;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.annotation.RequiresUser;
import org.apache.shiro.crypto.SecureRandomNumberGenerator;
import org.apache.shiro.crypto.hash.Md5Hash;
import org.apache.shiro.subject.Subject;
import org.springframework.web.bind.annotation.*;
import top.dragondove.boardroom.entity.*;
import top.dragondove.boardroom.service.PermissionService;
import top.dragondove.boardroom.service.RemindService;
import top.dragondove.boardroom.service.RoleService;
import top.dragondove.boardroom.service.UserService;
import top.dragondove.boardroom.util.BeanValidationUtils;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/user")
public class AccountController {

    @Resource
    private UserService userService;

    @Resource
    private RoleService roleService;

    @Resource
    private PermissionService permissionService;

    @Resource
    private RemindService remindService;

    @GetMapping("/info")
    // @RequiresUser
    public Msg getCurrentUserInfo() {
        return Msg.success().addExtra("user", userService.getCurrentUser());
    }

    @GetMapping("/roles")
    @RequiresUser
    public Msg getCurrentRoles() {
        List<Role> roles = roleService.getRolesByUserId(userService.getCurrentUser().getId());
        return Msg.success().addExtra("roles", roles);
    }

    @GetMapping("/permissions")
    @RequiresUser
    public Msg getCurrentPermissions() {
        List<Role> roles = roleService.getRolesByUserId(userService.getCurrentUser().getId());
        List<Permission> permissions = new ArrayList<>();
        for (Role role : roles) {
            permissions.addAll(permissionService.getPermissionsByRoleId(role.getId()));
        }
        return Msg.success().addExtra("permissions", permissions);
    }

    @GetMapping("/reminds")
    @RequiresUser
    public Msg getCurrentUserReminds() {
        return Msg.success().addExtra("reminds", remindService.getRemindsByUserId(userService.getCurrentUser().getId()));
    }

    @GetMapping("/reminds/unread")
    @RequiresUser
    public Msg getCurrentUserUnreadReminds() {
        Long id = userService.getCurrentUser().getId();
        List<Remind> unread = remindService.getUnreadRemindsByUserId(id);
        remindService.setRemindsAsReadByUserId(id);
        return Msg.success().addExtra("reminds", unread);
    }

    @PostMapping("/login")
    public Msg login(@ModelAttribute User user,
                     @RequestParam(defaultValue = "false") boolean rememberMe) {
        if (null == user) {
            return Msg.error("???????????????????????????");
        }
        UsernamePasswordToken token = new UsernamePasswordToken(user.getUsername(),
                user.getPassword(), rememberMe);
        Subject currentUser = SecurityUtils.getSubject();
        try {
            currentUser.login(token);
        } catch (UnknownAccountException e) {
            return Msg.error("??????????????????");
        } catch (IncorrectCredentialsException e) {
            return Msg.error("??????????????????");
        } catch (LockedAccountException e) {
            return Msg.error("???????????????");
        } catch (AuthenticationException e) {
            return Msg.error("???????????????" + e.getMessage()).addExtra("subject", token);
        }
        return Msg.success("????????????");
    }

    @PostMapping("/logout")
    // @RequiresUser
    public Msg logout() {
        Subject currentUser = SecurityUtils.getSubject();
        currentUser.logout();
        return Msg.success("????????????");
    }


    @PostMapping("/password")
    @RequiresUser
    public Msg changePassword(@RequestParam String oldPassword,
                              @RequestParam String newPassword) {
        User currentUser = userService.getCurrentUser();
        oldPassword = new Md5Hash(oldPassword, currentUser.getSalt(), 3).toString();
        if (!oldPassword.equals(currentUser.getPassword())) {
            return Msg.error("???????????????");
        }
        if (BeanValidationUtils.invalidatePassword(newPassword)) {
            return Msg.error("?????????????????????????????????????????????6-16???");
        }
        String salt = new SecureRandomNumberGenerator().nextBytes().toHex();
        currentUser.setSalt(salt);
        currentUser.setPassword(new Md5Hash(newPassword, salt, 3).toString());
        userService.saveUser(currentUser);
        return Msg.success("??????????????????");
    }

}
