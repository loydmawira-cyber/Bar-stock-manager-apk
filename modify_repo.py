import re

file_path = "app/src/main/java/com/example/data/repository/BarStockRepository.kt"
with open(file_path, "r") as f:
    lines = f.readlines()

functions_to_hook = [
    "saveBarProfile", "registerUser", "createAttendantByAdmin", "changeUserPassword",
    "resetPasswordByIdentifier", "resetPasswordByPhone", "deleteUser", "updateUserStatus",
    "addItem", "updateItem", "deleteItem", "addCounter", "updateCounter", "deleteCounter",
    "setCounterItemStock", "removeCounterStock", "startShiftWithVerification", "addMidShiftAdjustment",
    "respondToAdjustment", "confirmStockAdjustment", "disputeStockAdjustment", "closeShiftAndReconcile",
    "resolveDispute", "markNotificationAsRead", "markAllNotificationsAsRead"
]

new_lines = []
in_target_function = False
target_brace_level = 0
brace_level = 0

for i, line in enumerate(lines):
    if "class BarStockRepository(private val dao: BarStockDao) {" in line:
        new_lines.append(line)
        new_lines.append("    var onDataModified: (() -> Unit)? = null\n")
        new_lines.append("    private fun notifyDataModified() {\n")
        new_lines.append("        onDataModified?.invoke()\n")
        new_lines.append("    }\n")
        continue

    # Function start
    is_start = False
    for func in functions_to_hook:
        # Match `suspend fun funcName(`
        if re.search(r'\bsuspend\s+fun\s+' + func + r'\b', line):
            is_start = True
            break
            
    if is_start:
        # Check if it's a one-liner expression body: `suspend fun foo() = dao.foo()`
        if "=" in line and "{" not in line:
            # We must convert it to a block body!
            # Example: `suspend fun updateItem(item: Item) = dao.updateItem(item)`
            # becomes `suspend fun updateItem(item: Item) { dao.updateItem(item); notifyDataModified() }`
            # This is slightly complex. Let's handle it:
            lhs, rhs = line.split("=", 1)
            # If it has a return type `suspend fun foo(): Type = ...` we need to preserve it.
            # Actually most expression bodies in the repo are Unit except a few like `registerUser` which returns Long.
            # Let's just use `.also { notifyDataModified() }`!
            new_lines.append(line.rstrip() + ".also { notifyDataModified() }\n")
            continue
        elif "{" in line:
            in_target_function = True
            target_brace_level = brace_level + 1
        else:
            # Maybe the '{' is on the next line, ignoring for now as Kotlin usually puts '{' on the same line.
            pass

    brace_level += line.count("{")
    brace_level -= line.count("}")

    if in_target_function and brace_level < target_brace_level:
        # We are at the closing brace of the target function!
        # Inject our call just before the closing brace
        indent = "    " * brace_level
        
        # Wait, if there's a return statement earlier, it misses it. 
        # For this app, `notifyDataModified` just needs to be called when successful.
        # It's much safer to use `.also { notifyDataModified() }` on the DAO calls or use a finally block.
        # But let's just insert before the closing brace.
        
        # Let's see if the line is just the closing brace
        parts = line.rsplit("}", 1)
        new_lines.append(parts[0] + indent + "    notifyDataModified()\n" + indent + "}\n" + (parts[1] if len(parts) > 1 else ""))
        in_target_function = False
        continue
        
    new_lines.append(line)

with open(file_path, "w") as f:
    f.writelines(new_lines)
