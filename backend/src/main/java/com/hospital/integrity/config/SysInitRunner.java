package com.hospital.integrity.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hospital.integrity.entity.*;
import com.hospital.integrity.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 系统初始化：首次启动时创建根科室、六类角色、管理员账号、菜单及角色-菜单。
 * 幂等：已存在 admin 账号则跳过。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SysInitRunner implements ApplicationRunner {

    private final SysDeptMapper sysDeptMapper;
    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysMenuMapper sysMenuMapper;
    private final SysRoleMenuMapper sysRoleMenuMapper;
    private final PasswordEncoder passwordEncoder;

    /** 菜单种子：key, 名称, 类型(M/C), 路径, 组件, 图标, 排序, 父key */
    private record MenuSeed(String key, String name, String type, String path, String component,
                            String icon, int sort, String parent) {
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Long adminCount = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, "admin"));
        if (adminCount != null && adminCount > 0) {
            log.info("系统已初始化，跳过种子数据");
            return;
        }
        log.info("开始初始化系统种子数据...");

        // 1. 根科室
        SysDept root = new SysDept();
        root.setParentId(0L);
        root.setDeptName("医院");
        root.setDeptCode("ROOT");
        root.setSortOrder(1);
        root.setStatus(1);
        sysDeptMapper.insert(root);

        // 2. 角色
        Map<String, Long> roleIds = new LinkedHashMap<>();
        roleIds.putAll(insertRole("admin", "系统管理员"));
        roleIds.putAll(insertRole("doctor", "普通医护人员"));
        roleIds.putAll(insertRole("dept_admin", "科室管理员"));
        roleIds.putAll(insertRole("auditor", "科研科审核员"));
        roleIds.putAll(insertRole("committee", "科研诚信委员会"));
        roleIds.putAll(insertRole("leader", "院领导"));

        // 3. 管理员账号（初始密码 admin123）
        SysUser admin = new SysUser();
        admin.setEmpNo("0000");
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin123"));
        admin.setRealName("系统管理员");
        admin.setDeptId(root.getDeptId());
        admin.setStatus(1);
        sysUserMapper.insert(admin);
        bindRole(admin.getUserId(), roleIds.get("admin"));

        // 4. 菜单
        List<MenuSeed> seeds = buildMenus();
        Map<String, Long> menuIds = new LinkedHashMap<>();
        for (MenuSeed s : seeds) {
            SysMenu menu = new SysMenu();
            menu.setParentId(s.parent() == null ? 0L : menuIds.get(s.parent()));
            menu.setMenuName(s.name());
            menu.setMenuType(s.type());
            menu.setPath(s.path());
            menu.setComponent(s.component());
            menu.setIcon(s.icon());
            menu.setSortOrder(s.sort());
            menu.setStatus(1);
            sysMenuMapper.insert(menu);
            menuIds.put(s.key(), menu.getMenuId());
        }

        // 5. 角色-菜单
        Map<String, Set<String>> roleMenus = buildRoleMenus();
        roleMenus.forEach((roleKey, keys) -> {
            Long roleId = roleIds.get(roleKey);
            if (roleId == null) {
                return;
            }
            keys.forEach(k -> {
                Long menuId = menuIds.get(k);
                if (menuId != null) {
                    SysRoleMenu rm = new SysRoleMenu();
                    rm.setRoleId(roleId);
                    rm.setMenuId(menuId);
                    sysRoleMenuMapper.insert(rm);
                }
            });
        });

        log.info("系统初始化完成：角色{}个，菜单{}个", roleIds.size(), menuIds.size());
    }

    private Map<String, Long> insertRole(String key, String name) {
        SysRole role = new SysRole();
        role.setRoleName(name);
        role.setRoleKey(key);
        role.setDataScope(3);
        role.setStatus(1);
        sysRoleMapper.insert(role);
        return Map.of(key, role.getRoleId());
    }

    private void bindRole(Long userId, Long roleId) {
        SysUserRole ur = new SysUserRole();
        ur.setUserId(userId);
        ur.setRoleId(roleId);
        sysUserRoleMapper.insert(ur);
    }

    private List<MenuSeed> buildMenus() {
        List<MenuSeed> list = new ArrayList<>();
        list.add(new MenuSeed("dash", "工作台", "M", "/dashboard", null, "Odometer", 1, null));
        list.add(new MenuSeed("mydash", "个人工作台", "C", "/dashboard/my", "dashboard/MyDashboard", null, 1, "dash"));
        list.add(new MenuSeed("deptdash", "科室看板", "C", "/dashboard/dept", "dashboard/DeptDashboard", null, 2, "dash"));
        list.add(new MenuSeed("hospdash", "全院看板", "C", "/dashboard/hospital", "dashboard/HospitalDashboard", null, 3, "dash"));

        list.add(new MenuSeed("ach", "成果管理", "M", "/achievement", null, "Document", 2, null));
        list.add(new MenuSeed("myach", "我的成果", "C", "/achievement/my", "achievement/MyAchievements", null, 1, "ach"));
        list.add(new MenuSeed("auditach", "成果审核", "C", "/achievement/audit", "achievement/AuditAchievements", null, 2, "ach"));

        list.add(new MenuSeed("int", "诚信评价", "M", "/integrity", null, "Medal", 3, null));
        list.add(new MenuSeed("myint", "我的诚信档案", "C", "/integrity/my", "integrity/MyIntegrity", null, 1, "int"));
        list.add(new MenuSeed("intadmin", "评价管理", "C", "/integrity/admin", "integrity/IntegrityAdmin", null, 2, "int"));

        list.add(new MenuSeed("risk", "风险与处置", "M", "/risk", null, "Warning", 4, null));
        list.add(new MenuSeed("risklog", "风险预警", "C", "/risk/log", "risk/RiskLog", null, 1, "risk"));
        list.add(new MenuSeed("check", "核查工单", "C", "/risk/check", "risk/CheckList", null, 2, "risk"));
        list.add(new MenuSeed("viol", "违规与整改", "C", "/risk/violation", "risk/ViolationList", null, 3, "risk"));

        list.add(new MenuSeed("appeal", "申诉中心", "M", "/appeal", null, "ChatLineRound", 5, null));
        list.add(new MenuSeed("appeallist", "我的申诉", "C", "/appeal/list", "appeal/AppealList", null, 1, "appeal"));

        list.add(new MenuSeed("sys", "系统管理", "M", "/system", null, "Setting", 6, null));
        list.add(new MenuSeed("user", "用户管理", "C", "/system/user", "system/UserManage", null, 1, "sys"));
        list.add(new MenuSeed("dept", "科室管理", "C", "/system/dept", "system/DeptManage", null, 2, "sys"));
        list.add(new MenuSeed("role", "角色权限", "C", "/system/role", "system/RoleManage", null, 3, "sys"));
        list.add(new MenuSeed("menu", "菜单管理", "C", "/system/menu", "system/MenuManage", null, 4, "sys"));
        list.add(new MenuSeed("dict", "字典管理", "C", "/system/dict", "system/DictManage", null, 5, "sys"));
        list.add(new MenuSeed("rule", "规则配置", "C", "/system/rule", "system/RuleManage", null, 6, "sys"));
        list.add(new MenuSeed("black", "黑名单管理", "C", "/system/blacklist", "system/BlacklistManage", null, 7, "sys"));
        list.add(new MenuSeed("log", "日志审计", "C", "/system/log", "system/LogManage", null, 8, "sys"));

        list.add(new MenuSeed("notice", "通知中心", "C", "/notice", "notice/index", "Bell", 7, null));
        list.add(new MenuSeed("profile", "个人中心", "C", "/profile", "profile/index", "User", 8, null));
        return list;
    }

    private Map<String, Set<String>> buildRoleMenus() {
        Map<String, Set<String>> map = new HashMap<>();
        map.put("admin", Set.of("dash", "mydash", "deptdash", "hospdash", "ach", "myach", "auditach",
                "int", "myint", "intadmin", "risk", "risklog", "check", "viol", "appeal", "appeallist",
                "sys", "user", "dept", "role", "menu", "dict", "rule", "black", "log", "notice", "profile"));
        map.put("doctor", Set.of("dash", "mydash", "ach", "myach", "int", "myint",
                "appeal", "appeallist", "notice", "profile"));
        map.put("dept_admin", Set.of("dash", "mydash", "deptdash", "ach", "myach", "auditach",
                "int", "myint", "appeal", "appeallist", "notice", "profile"));
        map.put("auditor", Set.of("dash", "mydash", "deptdash", "ach", "myach", "auditach",
                "int", "myint", "intadmin", "risk", "risklog", "check", "viol",
                "appeal", "appeallist", "notice", "profile"));
        map.put("committee", Set.of("dash", "mydash", "ach", "myach", "int", "myint", "intadmin",
                "risk", "risklog", "check", "viol", "appeal", "appeallist",
                "sys", "rule", "black", "notice", "profile"));
        map.put("leader", Set.of("dash", "mydash", "deptdash", "hospdash", "int", "myint", "intadmin",
                "risk", "viol", "notice", "profile"));
        return map;
    }
}
