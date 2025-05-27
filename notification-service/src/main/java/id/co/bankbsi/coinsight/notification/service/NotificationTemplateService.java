package id.co.bankbsi.coinsight.notification.service;

import id.co.bankbsi.coinsight.notification.event.BudgetAlertEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationTemplateService {

  private DecimalFormat rupiahFormatter;

  {
    DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("id", "ID"));
    symbols.setGroupingSeparator('.');
    symbols.setDecimalSeparator(',');
    rupiahFormatter = new DecimalFormat("#,##0.00", symbols);
  }

  private String formatRupiah(BigDecimal amount) {
    return "Rp" + rupiahFormatter.format(amount);
  }

  public String generateSubject(String templateName, BudgetAlertEvent event) {
    return switch (templateName) {
      case "BUDGET_WARNING" -> String.format(
          "Budget Alert: %s - %d%% Used", event.getBudgetName(), event.getThresholdPercentage());
      case "BUDGET_EXCEEDED" -> String.format("Budget Exceeded: %s", event.getBudgetName());
      case "BUDGET_INFO" -> String.format("Budget Update: %s", event.getBudgetName());
      default -> "Budget Notification";
    };
  }

  public String generateContent(String templateName, BudgetAlertEvent event) {
    BigDecimal percentage =
        event
            .getCurrentSpent()
            .divide(event.getBudgetLimit(), 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));

    return switch (templateName) {
      case "BUDGET_WARNING" -> String.format(
          """
                Hi there!

                Your budget "%s" for %s has reached %d%% of your limit.

                Budget Details:
                • Budget Limit: %s
                • Current Spent: %s
                • Remaining: %s
                • Latest Transaction: %s

                Consider reviewing your spending to stay within your budget.

                Best regards,
                CoinSight Team
                """,
          event.getBudgetName(),
          event.getCategoryName(),
          percentage.intValue(),
          formatRupiah(event.getBudgetLimit()),
          formatRupiah(event.getCurrentSpent()),
          formatRupiah(event.getBudgetLimit().subtract(event.getCurrentSpent())),
          formatRupiah(event.getTransactionAmount()));

      case "BUDGET_EXCEEDED" -> String.format(
          """
                Hi there!

                Your budget "%s" for %s has been exceeded!

                Budget Details:
                • Budget Limit: %s
                • Current Spent: %s
                • Over Budget: %s
                • Latest Transaction: %s

                You might want to review your spending or adjust your budget.

                Best regards,
                CoinSight Team
                """,
          event.getBudgetName(),
          event.getCategoryName(),
          formatRupiah(event.getBudgetLimit()),
          formatRupiah(event.getCurrentSpent()),
          formatRupiah(event.getCurrentSpent().subtract(event.getBudgetLimit())),
          formatRupiah(event.getTransactionAmount()));

      case "BUDGET_INFO" -> String.format(
          """
                Hi there!

                Your budget "%s" for %s has been updated.

                Budget Details:
                • Budget Limit: %s
                • Current Spent: %s (%.1f%%)
                • Remaining: %s
                • Latest Transaction: %s

                Keep up the good work tracking your expenses!

                Best regards,
                CoinSight Team
                """,
          event.getBudgetName(),
          event.getCategoryName(),
          formatRupiah(event.getBudgetLimit()),
          formatRupiah(event.getCurrentSpent()),
          percentage,
          formatRupiah(event.getBudgetLimit().subtract(event.getCurrentSpent())),
          formatRupiah(event.getTransactionAmount()));

      default -> "Budget notification update.";
    };
  }
}
