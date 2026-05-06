package dk.rfg.fleetmanager.config;

import jakarta.persistence.OptimisticLockException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Catches optimistic locking conflicts — when two dispatchers save the
     * same task simultaneously. Redirects back with a clear Danish message
     * instead of a 500 error page.
     */
    @ExceptionHandler({
        OptimisticLockException.class,
        ObjectOptimisticLockingFailureException.class
    })
    public String handleOptimisticLock(Exception ex,
                                       RedirectAttributes attrs,
                                       HttpServletRequest request) {
        attrs.addFlashAttribute("errorMessage",
            "En anden bruger har lige redigeret denne post. " +
            "Siden er blevet genindlæst — tjek ændringerne og gem igen.");
        // Redirect back to wherever they came from
        String referer = request.getHeader("Referer");
        if (referer != null && referer.contains("/tasks")) return "redirect:/tasks";
        if (referer != null && referer.contains("/vehicles")) return "redirect:/vehicles";
        return "redirect:/tasks";
    }
}
