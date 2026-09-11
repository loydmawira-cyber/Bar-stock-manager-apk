file_path = "app/src/main/java/com/example/ui/viewmodel/BarStockViewModel.kt"
content = File.read(file_path)

mutators = [
  "saveBarProfile",
  "registerUser",
  "createAttendantByAdmin",
  "updateUserStatus",
  "deleteUser",
  "changeUserPassword",
  "resetPasswordByPhone",
  "startShiftWithVerification",
  "confirmStockAdjustment",
  "disputeStockAdjustment",
  "closeShiftAndReconcile",
  "resolveDispute",
  "addMidShiftAdjustment",
  "addCounter",
  "updateCounter",
  "deleteCounter",
  "setCounterItemStock",
  "addItem",
  "updateItem",
  "deleteItem",
  "markNotificationAsRead",
  "markAllNotificationsAsRead"
]

lines = content.split("\n")
new_lines = []

mutators.each do |mutator|
  lines.each_with_index do |line, index|
    if line.include?("repository.#{mutator}(") || line.match(/repository\.#{mutator}\b/)
      # If the next line doesn't already have triggerAutoBackup()
      if index + 1 < lines.length && !lines[index+1].include?("triggerAutoBackup")
        # We need to find if this is a multi-line method call
        # Let's just insert triggerAutoBackup() right before the `_toastMessage.emit` for these functions
      end
    end
  end
end
