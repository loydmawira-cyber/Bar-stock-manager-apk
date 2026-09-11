import re

file_path = "app/src/main/java/com/example/data/repository/BarStockRepository.kt"
with open(file_path, "r") as f:
    content = f.read()

# Add the callback
content = content.replace("class BarStockRepository(private val dao: BarStockDao) {", "class BarStockRepository(private val dao: BarStockDao) {\n\n    var onDataModified: (() -> Unit)? = null\n\n    private fun notifyDataModified() {\n        onDataModified?.invoke()\n    }")

# Now add notifyDataModified() before every return in mutating suspend functions.
# To be safe and simple, let's just replace all `dao.insert`, `dao.update`, `dao.delete` calls
# to also trigger the callback. But they are often expressions like `= dao.updateItem(item)`
# Let's write a wrapper inside BarStockRepository:

# Actually, finding functions is easier.
functions_to_hook = [
    "saveBarProfile", "registerUser", "createAttendantByAdmin", "changeUserPassword",
    "resetPasswordByIdentifier", "resetPasswordByPhone", "deleteUser", "updateUserStatus",
    "addItem", "updateItem", "deleteItem", "addCounter", "updateCounter", "deleteCounter",
    "setCounterItemStock", "removeCounterStock", "startShiftWithVerification", "addMidShiftAdjustment",
    "respondToAdjustment", "confirmStockAdjustment", "disputeStockAdjustment", "closeShiftAndReconcile",
    "resolveDispute", "markNotificationAsRead", "markAllNotificationsAsRead"
]

